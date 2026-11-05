package org.study.jibeo.global.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import team.themoment.sdk.exception.ExpectedException;

/**
 * JWT 필터가 SecurityContext 에 넣어 둔 로그인 사용자 정보를 꺼낸다.
 * principal = userId, details = companyId (JwtAuthenticationFilter 참고)
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        Object principal = authentication().getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        throw unauthorized();
    }

    public static Long companyId() {
        Object details = authentication().getDetails();
        if (details instanceof Long companyId) {
            return companyId;
        }
        throw unauthorized();
    }

    private static Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw unauthorized();
        }
        return authentication;
    }

    private static ExpectedException unauthorized() {
        return new ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED);
    }
}
