package org.study.jibeo.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.study.jibeo.global.validation.Phone;

public record UpdateMeRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Phone String phone,
        @Email @Size(max = 100) String email
) {
}
