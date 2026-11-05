package org.study.jibeo.domain.repairhistory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.study.jibeo.global.entity.BaseEntity;
import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.part.model.Part;
import org.study.jibeo.domain.request.model.Request;

import java.time.LocalDate;

/**
 * 제작 업체가 호출을 처리한 뒤 남기는 수리 기록. 같은 모델의 상담·솔루션 초안의 근거가 된다.
 */
@Getter
@Entity
@Table(name = "repair_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RepairHistory extends BaseEntity {

    /** 고장 부품 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    /** 수리한 제작 업체 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @Column(columnDefinition = "TEXT")
    private String cause;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RepairDifficulty difficulty;

    @Column(name = "repaired_at", nullable = false)
    private LocalDate repairedAt;

    @Builder
    private RepairHistory(Part part, Equipment equipment, Company company, Request request, String solution,
                          String cause, RepairDifficulty difficulty, LocalDate repairedAt) {
        this.part = part;
        this.equipment = equipment;
        this.company = company;
        this.request = request;
        this.solution = solution;
        this.cause = cause;
        this.difficulty = difficulty;
        this.repairedAt = repairedAt;
    }
}
