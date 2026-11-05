package org.study.jibeo.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.study.jibeo.global.validation.Phone;

/**
 * 가입하면 역할 미정(PENDING)이다. 역할과 소속 회사는 로그인 후 POST /api/me/role 로 정한다.
 */
public record SignUpRequest(
        @NotBlank @Size(min = 4, max = 50) String loginId,
        @NotBlank @Size(min = 8, max = 64) String pw,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Phone String phone,
        @Email @Size(max = 100) String email
) {
}
