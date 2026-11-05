package org.study.jibeo.domain.consultation.model;

public enum MessageType {
    /** 사용자가 처음 입력한 증상 */
    SYMPTOM,
    /** AI 질문 */
    QUESTION,
    /** 사용자 답변 */
    ANSWER,
    /** AI 해결책 */
    SOLUTION,
    /** AI 가 원인을 좁히지 못해 담당자 호출을 권함 */
    ESCALATE
}
