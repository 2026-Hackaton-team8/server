package org.study.jibeo.domain.user.model;

public enum UserRole {
    /** 가입 직후 역할 미정. POST /api/me/role 로 역할과 소속 회사를 한 번 정한다 */
    PENDING,
    /** 사용 업체 직원 */
    WORKER,
    /** 제작 업체 직원 */
    STAFF
}
