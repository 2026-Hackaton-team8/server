package org.study.jibeo.domain.request.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** 엔지니어 배정 (재배정도 같은 API) */
public record AssignRequest(
        @NotNull Long engineerId,
        @NotNull LocalDateTime visitAt
) {
}
