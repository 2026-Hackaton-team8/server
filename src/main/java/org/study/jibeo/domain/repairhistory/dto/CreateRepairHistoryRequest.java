package org.study.jibeo.domain.repairhistory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.study.jibeo.domain.repairhistory.model.RepairDifficulty;

/** 수리 이력 기록. 수리일은 기록한 날(한국 시간)로 저장한다 */
public record CreateRepairHistoryRequest(
        @NotNull Long partId,
        @NotBlank @Size(max = 2000) String cause,
        @NotBlank @Size(max = 4000) String solution,
        @NotNull RepairDifficulty difficulty
) {
}
