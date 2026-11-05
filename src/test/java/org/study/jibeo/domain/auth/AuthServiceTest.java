package org.study.jibeo.domain.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.study.jibeo.domain.auth.dto.LoginRequest;
import org.study.jibeo.domain.auth.dto.LoginResponse;
import org.study.jibeo.domain.auth.dto.RefreshResponse;
import org.study.jibeo.domain.auth.dto.SignUpRequest;
import org.study.jibeo.domain.auth.dto.SignUpResponse;
import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.domain.company.model.CompanyType;
import org.study.jibeo.domain.company.repository.CompanyRepository;
import org.study.jibeo.domain.user.dto.SelectRoleRequest;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.domain.user.model.UserRole;
import org.study.jibeo.domain.user.repository.UserRepository;
import org.study.jibeo.global.config.jwt.JwtProperties;
import org.study.jibeo.global.config.jwt.JwtTokenProvider;
import team.themoment.sdk.exception.ExpectedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long REFRESH_TTL = 120_000L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private RefreshTokenStore refreshTokenStore;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(
            new JwtProperties("test-secret-key-for-unit-tests-must-be-at-least-32-bytes-long", 60_000L, REFRESH_TTL));

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, companyRepository, passwordEncoder, tokenProvider, refreshTokenStore);
    }

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        void 역할_미정으로_비밀번호를_암호화해서_저장한다() {
            given(userRepository.existsByLoginId("worker1")).willReturn(false);
            given(userRepository.save(any(User.class))).willAnswer(inv -> withId(inv.getArgument(0), 10L));

            SignUpResponse response = authService.signUp(signUp("worker1"));

            assertThat(response.userId()).isEqualTo(10L);
            assertThat(response.role()).isEqualTo(UserRole.PENDING);
            assertThat(response.companyId()).isNull();
            verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(u ->
                    u.getRole() == UserRole.PENDING && u.getCompany() == null
                            && passwordEncoder.matches("password123", u.getPw())));
        }

        @Test
        void 아이디가_중복이면_400() {
            given(userRepository.existsByLoginId("worker1")).willReturn(true);

            assertExpected(() -> authService.signUp(signUp("worker1")),
                    HttpStatus.BAD_REQUEST, "이미 사용 중인 아이디입니다.");
        }

        @Test
        void 연락처를_함께_저장한다() {
            given(userRepository.existsByLoginId("worker1")).willReturn(false);
            given(userRepository.save(any(User.class))).willAnswer(inv -> withId(inv.getArgument(0), 10L));

            authService.signUp(new SignUpRequest("worker1", "password123", "홍길동", "010-1234-5678", "hong@example.com"));

            verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(u ->
                    u.getName().equals("홍길동") && u.getPhone().equals("010-1234-5678")
                            && "hong@example.com".equals(u.getEmail())));
        }
    }

    @Nested
    @DisplayName("역할 선택")
    class SelectRole {

        @Test
        void 역할과_회사를_정하고_새_토큰을_발급한다() {
            User user = pendingUser(10L);
            given(userRepository.findById(10L)).willReturn(Optional.of(user));
            given(companyRepository.findById(1L)).willReturn(Optional.of(company(1L, CompanyType.USER_COMPANY)));

            LoginResponse response = authService.selectRole(10L, new SelectRoleRequest(UserRole.WORKER, 1L));

            assertThat(user.getRole()).isEqualTo(UserRole.WORKER);
            assertThat(user.getCompanyId()).isEqualTo(1L);
            assertThat(response.role()).isEqualTo(UserRole.WORKER);
            assertThat(response.companyId()).isEqualTo(1L);
            var claims = tokenProvider.parse(response.accessToken()).getPayload();
            assertThat(claims.get("role", String.class)).isEqualTo("WORKER");
            assertThat(claims.get("companyId", Long.class)).isEqualTo(1L);
            verify(refreshTokenStore).save(10L, response.refreshToken(), REFRESH_TTL);
        }

        @Test
        void 회사가_없으면_400() {
            given(userRepository.findById(10L)).willReturn(Optional.of(pendingUser(10L)));
            given(companyRepository.findById(99L)).willReturn(Optional.empty());

            assertExpected(() -> authService.selectRole(10L, new SelectRoleRequest(UserRole.WORKER, 99L)),
                    HttpStatus.BAD_REQUEST, "존재하지 않는 회사입니다.");
        }

        @Test
        void 회사_타입과_역할이_맞지_않으면_400() {
            User user = pendingUser(10L);
            given(userRepository.findById(10L)).willReturn(Optional.of(user));
            given(companyRepository.findById(1L)).willReturn(Optional.of(company(1L, CompanyType.MAKER_COMPANY)));

            assertExpected(() -> authService.selectRole(10L, new SelectRoleRequest(UserRole.WORKER, 1L)),
                    HttpStatus.BAD_REQUEST, "회사 타입과 역할이 맞지 않습니다.");
            assertThat(user.getRole()).isEqualTo(UserRole.PENDING);
        }

        @Test
        void PENDING_으로는_바꿀_수_없다() {
            given(userRepository.findById(10L)).willReturn(Optional.of(pendingUser(10L)));
            given(companyRepository.findById(1L)).willReturn(Optional.of(company(1L, CompanyType.USER_COMPANY)));

            assertExpected(() -> authService.selectRole(10L, new SelectRoleRequest(UserRole.PENDING, 1L)),
                    HttpStatus.BAD_REQUEST, "회사 타입과 역할이 맞지 않습니다.");
        }

        @Test
        void 이미_역할이_있으면_409() {
            given(userRepository.findById(10L)).willReturn(Optional.of(user(10L, "worker1", "password123")));
            given(companyRepository.findById(1L)).willReturn(Optional.of(company(1L, CompanyType.USER_COMPANY)));

            assertExpected(() -> authService.selectRole(10L, new SelectRoleRequest(UserRole.WORKER, 1L)),
                    HttpStatus.CONFLICT, "이미 역할이 정해진 사용자입니다.");
            verify(refreshTokenStore, never()).save(anyLong(), anyString(), anyLong());
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        void 성공하면_토큰을_발급하고_리프레시_토큰을_저장한다() {
            User user = user(10L, "worker1", "password123");
            given(userRepository.findByLoginId("worker1")).willReturn(Optional.of(user));

            LoginResponse response = authService.login(new LoginRequest("worker1", "password123"));

            assertThat(response.userId()).isEqualTo(10L);
            assertThat(tokenProvider.parse(response.accessToken()).getPayload().getSubject()).isEqualTo("10");
            assertThat(tokenProvider.isRefreshToken(tokenProvider.parse(response.refreshToken()).getPayload())).isTrue();
            verify(refreshTokenStore).save(10L, response.refreshToken(), REFRESH_TTL);
        }

        @Test
        void 없는_아이디면_401() {
            given(userRepository.findByLoginId("nobody")).willReturn(Optional.empty());

            assertExpected(() -> authService.login(new LoginRequest("nobody", "password123")),
                    HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        @Test
        void 비밀번호가_틀리면_401() {
            given(userRepository.findByLoginId("worker1")).willReturn(Optional.of(user(10L, "worker1", "password123")));

            assertExpected(() -> authService.login(new LoginRequest("worker1", "wrong-password")),
                    HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
            verify(refreshTokenStore, never()).save(anyLong(), anyString(), anyLong());
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Refresh {

        @Test
        void 저장된_토큰과_같으면_새_토큰을_발급한다() {
            User user = user(10L, "worker1", "password123");
            String refreshToken = tokenProvider.createRefreshToken(10L);
            given(refreshTokenStore.find(10L)).willReturn(Optional.of(refreshToken));
            given(userRepository.findById(10L)).willReturn(Optional.of(user));

            RefreshResponse response = authService.refresh(refreshToken);

            assertThat(response.accessToken()).isNotBlank();
            verify(refreshTokenStore).save(eq(10L), eq(response.refreshToken()), eq(REFRESH_TTL));
        }

        @Test
        void 저장된_토큰과_다르면_저장된_토큰을_지우고_401() {
            String refreshToken = tokenProvider.createRefreshToken(10L);
            given(refreshTokenStore.find(10L)).willReturn(Optional.of("other-token"));

            assertExpected(() -> authService.refresh(refreshToken),
                    HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.");
            verify(refreshTokenStore).delete(10L);
        }

        @Test
        void 액세스_토큰으로_재발급하면_401() {
            String accessToken = tokenProvider.createAccessToken(10L, UserRole.WORKER, 1L);

            assertExpected(() -> authService.refresh(accessToken),
                    HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.");
        }

        @Test
        void 형식이_잘못된_토큰이면_401() {
            assertExpected(() -> authService.refresh("not-a-jwt"),
                    HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        void 리프레시_토큰이면_저장된_토큰을_삭제한다() {
            authService.logout(tokenProvider.createRefreshToken(10L));

            verify(refreshTokenStore).delete(10L);
        }

        @Test
        void 잘못된_토큰이어도_예외_없이_무시한다() {
            assertThatCode(() -> authService.logout("not-a-jwt")).doesNotThrowAnyException();
            verify(refreshTokenStore, never()).delete(anyLong());
        }
    }

    private static SignUpRequest signUp(String loginId) {
        return new SignUpRequest(loginId, "password123", "홍길동", "010-1234-5678", null);
    }

    private static User pendingUser(Long id) {
        return withId(User.builder()
                .loginId("pending1").pw("x").role(UserRole.PENDING).name("홍길동").phone("010-1234-5678")
                .build(), id);
    }

    private static void assertExpected(org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
                                       HttpStatus status, String message) {
        assertThatThrownBy(call)
                .isInstanceOf(ExpectedException.class)
                .hasMessage(message)
                .satisfies(e -> assertThat(((ExpectedException) e).getStatusCode()).isEqualTo(status));
    }

    private static Company company(Long id, CompanyType type) {
        Company company = Company.builder()
                .name("테스트 회사")
                .located("광주")
                .businessNumber("123-45-67890")
                .type(type)
                .phone("062-123-4567")
                .build();
        return withId(company, id);
    }

    private User user(Long id, String loginId, String rawPw) {
        User user = User.builder()
                .loginId(loginId)
                .pw(passwordEncoder.encode(rawPw))
                .role(UserRole.WORKER)
                .company(company(1L, CompanyType.USER_COMPANY))
                .name("홍길동")
                .phone("010-1234-5678")
                .build();
        return withId(user, id);
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
