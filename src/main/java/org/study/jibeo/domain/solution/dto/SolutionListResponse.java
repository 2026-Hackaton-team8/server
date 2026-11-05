package org.study.jibeo.domain.solution.dto;

import org.study.jibeo.domain.solution.model.Solution;
import org.study.jibeo.domain.solution.model.SolutionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 모델별 솔루션 목록. counts = 탭별 개수 (ALL, DRAFT, APPROVED, WITHDRAWN) */
public record SolutionListResponse(Map<String, Long> counts, List<Item> content) {

    public record Item(Long id, String title, String category, SolutionStatus status, int stepCount,
                       int evidenceCount, LocalDateTime approvedAt, LocalDateTime createdAt) {

        public static Item from(Solution s) {
            return new Item(s.getId(), s.getTitle(), s.getCategory(), s.getStatus(), s.getSteps().size(),
                    s.getEvidences().size(), s.getApprovedAt(), s.getCreatedAt());
        }
    }
}
