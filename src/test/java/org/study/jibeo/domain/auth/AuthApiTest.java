package org.study.jibeo.domain.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.domain.company.model.CompanyType;
import org.study.jibeo.domain.company.repository.CompanyRepository;
import org.study.jibeo.domain.user.model.UserRole;
import org.study.jibeo.global.config.jwt.JwtTokenProvider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private JwtTokenProvider tokenProvider;

    // Redis 없이 테스트하기 위해 리프레시 토큰 저장소는 mock 처리
    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @Test
    void 토큰_없이_보호된_API를_호출하면_SDK_형식의_401() throws Exception {
        mockMvc.perform(get("/api/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    void 잘못된_토큰이면_401() throws Exception {
        mockMvc.perform(get("/api/anything").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 유효한_토큰이면_인증을_통과한다() throws Exception {
        String token = tokenProvider.createAccessToken(1L, UserRole.WORKER, 1L);

        // 인증은 통과하고, 없는 경로라 404
        mockMvc.perform(get("/api/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void 가입하면_역할_미정이고_역할을_고르면_새_토큰으로_권한이_바뀐다() throws Exception {
        Long companyId = company("사용 업체", "123-45-67890", CompanyType.USER_COMPANY);
        Long makerId = company("제작사", "123-45-00000", CompanyType.MAKER_COMPANY);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"worker1","pw":"password123","name":"홍길동","phone":"010-1234-5678"}
                                """))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data.role").value("PENDING"))
                .andExpect(jsonPath("$.data.companyId").doesNotExist());

        String login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"worker1","pw":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("PENDING"))
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String pendingToken = JsonPath.read(login, "$.data.accessToken");

        // 역할 미정: 내 정보는 보이지만(회사 없음) 역할별 API는 403
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + pendingToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("PENDING"))
                .andExpect(jsonPath("$.data.company").doesNotExist());
        mockMvc.perform(get("/api/equipments").header("Authorization", "Bearer " + pendingToken))
                .andExpect(status().isForbidden());

        // 회사 타입과 맞지 않는 역할은 400
        mockMvc.perform(post("/api/me/role").header("Authorization", "Bearer " + pendingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"STAFF","companyId":%d}
                                """.formatted(companyId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("회사 타입과 역할이 맞지 않습니다."));

        String selected = mockMvc.perform(post("/api/me/role").header("Authorization", "Bearer " + pendingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"WORKER","companyId":%d}
                                """.formatted(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("WORKER"))
                .andExpect(jsonPath("$.data.companyId").value(companyId))
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String workerToken = JsonPath.read(selected, "$.data.accessToken");

        mockMvc.perform(get("/api/equipments").header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + workerToken))
                .andExpect(jsonPath("$.data.company.id").value(companyId));

        // 역할은 한 번만 고를 수 있다
        mockMvc.perform(post("/api/me/role").header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"STAFF","companyId":%d}
                                """.formatted(makerId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 역할이 정해진 사용자입니다."));
    }

    @Test
    void 역할_선택은_필수값을_검사한다() throws Exception {
        String token = tokenProvider.createAccessToken(1L, UserRole.PENDING, null);

        mockMvc.perform(post("/api/me/role").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void 유효성_검사에_실패하면_필드별_오류를_준다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 값이 유효하지 않습니다."))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void Swagger_문서에_JWT_인증_스키마가_있다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists());
    }

    @Test
    void Swagger_문서의_모든_API에_한글_요약이_있다() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.summary").value("회원가입"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.tags[0]").value("인증"))
                .andReturn().getResponse().getContentAsString();

        java.util.List<String> operations = JsonPath.read(json, "$.paths.*.*.operationId");
        java.util.List<String> summaries = JsonPath.read(json, "$.paths.*.*.summary");
        org.assertj.core.api.Assertions.assertThat(summaries)
                .as("summary 가 없는 API가 있습니다. 컨트롤러 메서드에 @Operation(summary = ...) 를 붙이세요.")
                .hasSameSizeAs(operations)
                .allMatch(s -> s.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HANGUL));
    }

    private Long company(String name, String businessNumber, CompanyType type) {
        return companyRepository.save(Company.builder()
                .name(name).located("광주").businessNumber(businessNumber).type(type).phone("062-123-4567")
                .build()).getId();
    }
}
