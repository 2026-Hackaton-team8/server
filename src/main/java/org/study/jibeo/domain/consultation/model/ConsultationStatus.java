package org.study.jibeo.domain.consultation.model;

public enum ConsultationStatus {
    /** 진행 중 */
    IN_PROGRESS,
    /** 담당자 호출로 넘어감 */
    ESCALATED,
    /** 해결됨 */
    RESOLVED
}
