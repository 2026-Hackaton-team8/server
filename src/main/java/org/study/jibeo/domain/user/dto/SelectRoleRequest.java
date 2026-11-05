package org.study.jibeo.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import org.study.jibeo.domain.user.model.UserRole;

/**
 * 역할 선택 (역할 미정일 때 한 번만). 회사 타입과 맞아야 한다: 사용 업체 → WORKER, 제작 업체 → STAFF.
 * 소속 회사는 GET /api/companies 에서 고르거나 POST /api/companies 로 먼저 등록한다.
 */
public record SelectRoleRequest(
        @NotNull UserRole role,
        @NotNull Long companyId
) {
}
