package org.study.jibeo.domain.request.model;

/**
 * 호출 상태. 제작 업체 호출 수신함의 탭과 1:1 대응
 * RECEIVED → ASSIGNED → COMPLETED, RECEIVED/ASSIGNED → SELF_RESOLVED
 */
public enum RequestStatus {
    /** 접수 (새 호출) */
    RECEIVED,
    /** 엔지니어 배정 */
    ASSIGNED,
    /** 수리 완료 */
    COMPLETED,
    /** 자가 해결 */
    SELF_RESOLVED;

    public boolean isOpen() {
        return this == RECEIVED || this == ASSIGNED;
    }
}
