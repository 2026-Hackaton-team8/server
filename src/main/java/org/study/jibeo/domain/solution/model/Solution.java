package org.study.jibeo.domain.solution.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.domain.repairhistory.model.RepairHistory;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.global.entity.BaseEntity;
import org.study.jibeo.global.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 수리 이력에서 만든, 이용 기업이 직접 해볼 수 있는 해결 방법. 승인되면 AI 상담의 근거가 된다.
 */
@Getter
@Entity
@Table(name = "solution")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Solution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_model_id", nullable = false)
    private EquipmentModel equipmentModel;

    /** 적용 상황, 예: 탈수가 안 되고 통에 물이 남을 때 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 계통, 예: 배수 계통 */
    @Column(nullable = false, length = 50)
    private String category;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> symptoms;

    /** 해결되지 않으면 안내 */
    @Column(columnDefinition = "TEXT")
    private String fallback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SolutionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "solution", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<SolutionStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "solution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolutionEvidence> evidences = new ArrayList<>();

    @Builder
    private Solution(EquipmentModel equipmentModel, String title, String category, List<String> symptoms,
                     String fallback, List<String> steps, List<RepairHistory> evidences) {
        this.equipmentModel = equipmentModel;
        this.status = SolutionStatus.DRAFT;
        replaceContent(title, category, symptoms, steps, fallback);
        evidences.forEach(h -> this.evidences.add(new SolutionEvidence(this, h)));
    }

    /** 초안·승인 상태에서 수정 (승인 상태는 그대로 유지) */
    public void update(String title, String category, List<String> symptoms, List<String> steps, String fallback) {
        if (status == SolutionStatus.WITHDRAWN) {
            throw ErrorCode.INVALID_SOLUTION_STATUS.toException();
        }
        replaceContent(title, category, symptoms, steps, fallback);
    }

    public void approve(User approver) {
        if (status == SolutionStatus.APPROVED) {
            throw ErrorCode.INVALID_SOLUTION_STATUS.toException();
        }
        this.status = SolutionStatus.APPROVED;
        this.approvedBy = approver;
        this.approvedAt = LocalDateTime.now();
    }

    public void withdraw() {
        if (status != SolutionStatus.APPROVED) {
            throw ErrorCode.INVALID_SOLUTION_STATUS.toException();
        }
        this.status = SolutionStatus.WITHDRAWN;
    }

    public boolean isOwnedBy(Long makerCompanyId) {
        return equipmentModel.isOwnedBy(makerCompanyId);
    }

    private void replaceContent(String title, String category, List<String> symptoms, List<String> steps,
                                String fallback) {
        this.title = title;
        this.category = category;
        this.symptoms = symptoms == null ? List.of() : List.copyOf(symptoms);
        this.fallback = fallback;
        this.steps.clear();
        for (int i = 0; i < steps.size(); i++) {
            this.steps.add(new SolutionStep(this, i + 1, steps.get(i)));
        }
    }
}
