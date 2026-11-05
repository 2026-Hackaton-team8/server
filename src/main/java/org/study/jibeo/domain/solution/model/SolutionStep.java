package org.study.jibeo.domain.solution.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.study.jibeo.global.entity.BaseEntity;

/** 이용 기업이 직접 해볼 단계 하나 */
@Getter
@Entity
@Table(name = "solution_step")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SolutionStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solution_id", nullable = false)
    private Solution solution;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(nullable = false, length = 500)
    private String content;

    SolutionStep(Solution solution, int stepOrder, String content) {
        this.solution = solution;
        this.stepOrder = stepOrder;
        this.content = content;
    }
}
