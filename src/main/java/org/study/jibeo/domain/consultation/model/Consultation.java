package org.study.jibeo.domain.consultation.model;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.part.model.Part;
import org.study.jibeo.domain.request.model.AiSummary;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.global.entity.BaseEntity;
import org.study.jibeo.global.exception.ErrorCode;

import java.time.LocalDateTime;

/**
 * AI 상담 1건. 대화는 consultation_message 에 쌓이고, AI 가 좁힌 원인·의심 부품·신고서 요약을 여기에 둔다.
 */
@Getter
@Entity
@Table(name = "consultation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consultation extends BaseEntity {

    public static final int MAX_QUESTIONS = 5;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    /** 상담한 사용 업체 (목록 조회용) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 처음 입력한 증상 (목록 제목) */
    @Column(nullable = false, length = 255)
    private String symptom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsultationStatus status;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    /** AI 가 좁힌 원인 */
    @Column(name = "ai_cause", length = 255)
    private String aiCause;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspected_part_id")
    private Part suspectedPart;

    /** 호출 신고서 자동 입력값 (AI 가 준 요약) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_summary")
    private AiSummary aiSummary;

    /** 해결 또는 호출 시각 */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Builder
    private Consultation(Equipment equipment, Company company, User user, String symptom) {
        this.equipment = equipment;
        this.company = company;
        this.user = user;
        this.symptom = symptom;
        this.status = ConsultationStatus.IN_PROGRESS;
        this.questionCount = 0;
    }

    public void countQuestion() {
        this.questionCount++;
    }

    /** AI 가 원인을 좁혔을 때 (해결책 또는 호출 권유) */
    public void updateFinding(String cause, Part suspectedPart, AiSummary summary) {
        this.aiCause = cause == null ? null : cause.length() > 255 ? cause.substring(0, 255) : cause;
        this.suspectedPart = suspectedPart;
        this.aiSummary = summary;
    }

    public void resolve() {
        requireInProgress();
        this.status = ConsultationStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void escalate() {
        requireInProgress();
        this.status = ConsultationStatus.ESCALATED;
        this.resolvedAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long companyId) {
        return company.getId().equals(companyId);
    }

    public void requireInProgress() {
        if (status != ConsultationStatus.IN_PROGRESS) {
            throw ErrorCode.CONSULTATION_CLOSED.toException();
        }
    }
}
