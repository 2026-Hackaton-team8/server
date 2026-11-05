package org.study.jibeo.domain.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.study.jibeo.domain.company.model.CompanyType;
import org.study.jibeo.global.validation.Phone;

public record CreateCompanyRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 255) String located,
        @NotBlank @Size(max = 50) String businessNumber,
        @NotNull CompanyType type,
        @NotBlank @Phone String phone,
        @Email @Size(max = 100) String email
) {
}