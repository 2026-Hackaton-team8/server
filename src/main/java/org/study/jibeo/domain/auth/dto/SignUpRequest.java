package org.study.jibeo.domain.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.study.jibeo.domain.company.model.CompanyType;
import org.study.jibeo.domain.user.model.UserRole;
import org.study.jibeo.global.validation.Phone;

/**
 * 회사는 companyId(기존 회사 선택) 또는 company(새 회사 등록) 중 하나만 보낸다.
 */
public record SignUpRequest(
        @NotBlank @Size(min = 4, max = 50) String loginId,
        @NotBlank @Size(min = 8, max = 64) String pw,
        @NotNull UserRole role,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Phone String phone,
        @Email @Size(max = 100) String email,
        Long companyId,
        @Valid NewCompany company
) {

    public record NewCompany(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 255) String located,
            @NotBlank @Size(max = 50) String businessNumber,
            @NotNull CompanyType type,
            @NotBlank @Phone String phone,
            @Email @Size(max = 100) String email
    ) {
    }
}
