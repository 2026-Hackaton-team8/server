package org.study.jibeo.domain.auth.dto;

import org.study.jibeo.domain.user.model.UserRole;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        UserRole role,
        Long companyId
) {
}
