package org.study.jibeo.domain.part.model;

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
import org.study.jibeo.domain.equipment.model.Equipment;

@Getter
@Entity
@Table(name = "part")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Part extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "ai_summary_solution", columnDefinition = "TEXT")
    private String aiSummarySolution;

    @Builder
    private Part(String name, Equipment equipment, String aiSummarySolution) {
        this.name = name;
        this.equipment = equipment;
        this.aiSummarySolution = aiSummarySolution;
    }
}
