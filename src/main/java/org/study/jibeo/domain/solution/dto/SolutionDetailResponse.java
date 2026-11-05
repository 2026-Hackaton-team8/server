package org.study.jibeo.domain.solution.dto;

import org.study.jibeo.domain.repairhistory.dto.RepairHistoryResponse;
import org.study.jibeo.domain.solution.model.Solution;
import org.study.jibeo.domain.solution.model.SolutionStatus;
import org.study.jibeo.domain.solution.model.SolutionStep;

import java.time.LocalDateTime;
import java.util.List;

/** 솔루션 상세: 적용 증상, 단계, fallback, 근거 수리 이력 */
public record SolutionDetailResponse(
        Long id,
        Long modelId,
        String modelCode,
        String title,
        String category,
        List<String> symptoms,
        List<String> steps,
        String fallback,
        SolutionStatus status,
        String approvedByName,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        List<RepairHistoryResponse> evidence
) {

    public static SolutionDetailResponse from(Solution s) {
        return new SolutionDetailResponse(s.getId(), s.getEquipmentModel().getId(), s.getEquipmentModel().getCode(),
                s.getTitle(), s.getCategory(), s.getSymptoms(),
                s.getSteps().stream().map(SolutionStep::getContent).toList(),
                s.getFallback(), s.getStatus(),
                s.getApprovedBy() == null ? null : s.getApprovedBy().getName(), s.getApprovedAt(), s.getCreatedAt(),
                s.getEvidences().stream().map(e -> RepairHistoryResponse.from(e.getRepairHistory())).toList());
    }
}
