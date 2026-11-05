package org.study.jibeo.domain.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import org.study.jibeo.global.config.jwt.JwtTokenProvider;
import org.study.jibeo.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    public boolean isLoginIdAvailable(String loginId) {
        return !userRepository.existsByLoginId(loginId);
    }

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw ErrorCode.DUPLICATE_LOGIN_ID.toException();
        }
        User saved = userRepository.save(User.builder()
                .loginId(request.loginId())
                .pw(passwordEncoder.encode(request.pw()))
                .role(UserRole.PENDING)
                .name(request.name())
                .phone(request.phone())
                .email(blankToNull(request.email()))
                .build());
        return new SignUpResponse(saved.getId(), saved.getLoginId(), saved.getRole(), null);
    }

    /**
     * 역할 미정 사용자가 역할과 소속 회사를 정한다 (한 번만).
     * 토큰에 역할·회사가 들어 있으므로 새 토큰을 발급해 돌려준다.
     */
    @Transactional
    public LoginResponse selectRole(Long userId, SelectRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(ErrorCode.COMPANY_NOT_FOUND::toException);
        validateRoleAgainstCompany(request.role(), company.getType());

        user.selectRole(request.role(), company);
        return issueTokens(user);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> ErrorCode.INVALID_CREDENTIALS.toException());
        if (!passwordEncoder.matches(request.pw(), user.getPw())) {
            throw ErrorCode.INVALID_CREDENTIALS.toException();
        }
        return issueTokens(user);
    }

    private LoginResponse issueTokens(User user) {
        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getRole(), user.getCompanyId());
        String refreshToken = tokenProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), refreshToken, tokenProvider.getRefreshTokenValidityMs());
        return new LoginResponse(accessToken, refreshToken, user.getId(), user.getRole(), user.getCompanyId());
    }

    public RefreshResponse refresh(String refreshToken) {
        Claims claims = parseRefresh(refreshToken);
        Long userId = Long.valueOf(claims.getSubject());

        String stored = refreshTokenStore.find(userId)
                .orElseThrow(() -> ErrorCode.INVALID_REFRESH_TOKEN.toException());
        if (!stored.equals(refreshToken)) {
            refreshTokenStore.delete(userId);
            throw ErrorCode.INVALID_REFRESH_TOKEN.toException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        String newAccess = tokenProvider.createAccessToken(user.getId(), user.getRole(), user.getCompanyId());
        String newRefresh = tokenProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), newRefresh, tokenProvider.getRefreshTokenValidityMs());
        return new RefreshResponse(newAccess, newRefresh);
    }

    public void logout(String refreshToken) {
        try {
            Claims claims = tokenProvider.parse(refreshToken).getPayload();
            if (!tokenProvider.isRefreshToken(claims)) {
                return;
            }
            Long userId = Long.valueOf(claims.getSubject());
            refreshTokenStore.delete(userId);
        } catch (JwtException | IllegalArgumentException ignored) {
        }
    }

    private Claims parseRefresh(String refreshToken) {
        try {
            Jws<Claims> jws = tokenProvider.parse(refreshToken);
            Claims claims = jws.getPayload();
            if (!tokenProvider.isRefreshToken(claims)) {
                throw ErrorCode.INVALID_REFRESH_TOKEN.toException();
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            throw ErrorCode.INVALID_REFRESH_TOKEN.toException();
        }
    }

    private void validateRoleAgainstCompany(UserRole role, CompanyType companyType) {
        boolean ok = (companyType == CompanyType.USER_COMPANY && role == UserRole.WORKER)
                || (companyType == CompanyType.MAKER_COMPANY && role == UserRole.STAFF);
        if (!ok) {
            throw ErrorCode.ROLE_COMPANY_MISMATCH.toException();
        }
    }
}
