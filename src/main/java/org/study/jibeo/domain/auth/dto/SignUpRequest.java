package org.study.jibeo.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.study.jibeo.domain.user.model.UserRole;
import org.study.jibeo.global.validation.Phone;

/**
 * 소속 회사는 미리 POST /api/companies 로 등록한 뒤 companyId 로 선택한다.
 */
public record SignUpRequest(
        @NotBlank @Size(min = 4, max = 50) String loginId,
        @NotBlank @Size(min = 8, max = 64) String pw,
        @NotNull UserRole role,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Phone String phone,
        @Email @Size(max = 100) String email,
        @NotNull Long companyId
) {
}