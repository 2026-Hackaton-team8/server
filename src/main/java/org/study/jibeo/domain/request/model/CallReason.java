package org.study.jibeo.domain.request.model;

/** 호출 사유. 상담 기록을 보고 서버가 정한다 (상담 연동 전에는 NEEDS_EXPERT) */
public enum CallReason {
    /** 제시한 해결책으로 해결되지 않음 */
    SOLUTION_FAILED,
    /** 전문가 작업이 필요함 */
    NEEDS_EXPERT,
    /** AI가 원인을 확신하지 못함 */
    LOW_CONFIDENCE,
    /** 참고할 수리 이력이 없음 */
    NO_HISTORY
}
