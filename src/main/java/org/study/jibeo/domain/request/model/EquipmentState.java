package org.study.jibeo.domain.request.model;

/** 호출 시점의 설비 상태 */
public enum EquipmentState {
    /** 완전히 멈춤 */
    STOPPED,
    /** 일부만 가동 */
    PARTIAL,
    /** 정상 가동 */
    NORMAL
}
