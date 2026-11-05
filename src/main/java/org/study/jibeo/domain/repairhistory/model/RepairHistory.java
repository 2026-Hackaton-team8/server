package org.study.jibeo.domain.repairhistory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@Entity
@Table(name = "repair_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RepairHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

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

    @Column(name = "repaired_at", nullable = false)
    private LocalDate repairedAt;

    @Builder
    private RepairHistory(Part part, Equipment equipment, Company company, Request request, String solution, String cause, LocalDate repairedAt) {
        this.part = part;
        this.equipment = equipment;
        this.company = company;
        this.request = request;
        this.solution = solution;
        this.cause = cause;
        this.repairedAt = repairedAt;
    }
}
