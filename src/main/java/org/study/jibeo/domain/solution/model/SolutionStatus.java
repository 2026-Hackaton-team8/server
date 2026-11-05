package org.study.jibeo.domain.solution.model;

/** DRAFT → APPROVED → WITHDRAWN (내린 솔루션은 다시 승인할 수 있다) */
public enum SolutionStatus {
    /** 검토 대기 */
    DRAFT,
    /** 승인됨: AI 상담의 근거로 쓰인다 */
    APPROVED,
    /** 내림 */
    WITHDRAWN
}
