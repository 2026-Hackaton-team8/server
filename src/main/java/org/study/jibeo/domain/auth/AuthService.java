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
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(ErrorCode.COMPANY_NOT_FOUND::toException);
        validateRoleAgainstCompany(request.role(), company.getType());

        User user = User.builder()
                .loginId(request.loginId())
                .pw(passwordEncoder.encode(request.pw()))
                .role(request.role())
                .company(company)
                .name(request.name())
                .phone(request.phone())
                .email(blankToNull(request.email()))
                .build();
        User saved = userRepository.save(user);
        return new SignUpResponse(saved.getId(), saved.getLoginId(), saved.getRole(), company.getId());
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
        Long companyId = user.getCompany().getId();
        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getRole(), companyId);
        String refreshToken = tokenProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), refreshToken, tokenProvider.getRefreshTokenValidityMs());
        return new LoginResponse(accessToken, refreshToken, user.getId(), user.getRole(), companyId);
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
        Long companyId = user.getCompany().getId();

        String newAccess = tokenProvider.createAccessToken(user.getId(), user.getRole(), companyId);
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
