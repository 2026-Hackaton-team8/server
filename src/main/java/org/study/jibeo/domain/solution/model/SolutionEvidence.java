package org.study.jibeo.domain.solution.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.study.jibeo.domain.repairhistory.model.RepairHistory;
import org.study.jibeo.global.entity.BaseEntity;

/** 솔루션의 근거로 쓴 수리 이력 */
@Getter
@Entity
@Table(name = "solution_evidence")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SolutionEvidence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solution_id", nullable = false)
    private Solution solution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repair_history_id", nullable = false)
    private RepairHistory repairHistory;

    SolutionEvidence(Solution solution, RepairHistory repairHistory) {
        this.solution = solution;
        this.repairHistory = repairHistory;
    }
}
