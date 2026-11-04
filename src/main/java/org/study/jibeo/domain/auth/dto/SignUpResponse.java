package org.study.jibeo.domain.auth.dto;

import org.study.jibeo.domain.user.model.UserRole;

public record SignUpResponse(Long userId, String loginId, UserRole role, Long companyId) {
}
