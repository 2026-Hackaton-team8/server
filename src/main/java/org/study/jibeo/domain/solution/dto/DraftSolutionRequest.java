package org.study.jibeo.domain.solution.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 초안의 근거로 쓸 이 모델의 수리 이력 */
public record DraftSolutionRequest(
        @NotEmpty @Size(max = 20) List<@NotNull Long> repairHistoryIds
) {
}
