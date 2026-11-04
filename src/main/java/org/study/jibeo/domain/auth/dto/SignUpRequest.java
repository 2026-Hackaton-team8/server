package org.study.jibeo.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.study.jibeo.domain.user.model.UserRole;

public record SignUpRequest(
        @NotBlank @Size(min = 4, max = 50) String loginId,
        @NotBlank @Size(min = 8, max = 64) String pw,
        @NotNull UserRole role,
        @NotNull Long companyId
) {
}
