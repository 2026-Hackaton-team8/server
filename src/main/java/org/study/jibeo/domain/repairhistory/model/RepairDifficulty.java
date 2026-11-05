package org.study.jibeo.domain.repairhistory.model;

/** 수리 난이도. AI 상담에서 이용 기업이 직접 해볼 수 있는 조치인지 구분하는 데 쓴다 */
public enum RepairDifficulty {
    /** 이용 기업이 직접 가능 */
    SELF,
    /** 엔지니어 작업 필요 */
    ENGINEER
}
