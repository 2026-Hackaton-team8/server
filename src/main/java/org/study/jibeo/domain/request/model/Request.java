package org.study.jibeo.domain.request.model;

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
import org.study.jibeo.domain.consultation.model.Consultation;
import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.global.entity.BaseEntity;
import org.study.jibeo.global.exception.ErrorCode;

import java.time.LocalDateTime;

/**
 * 담당자 호출 (수리 요청). 사용 업체가 설치 설비에 대해 제작 업체로 보낸다.
 * 연락처는 제출 시점 값을 저장해, 나중에 프로필을 바꿔도 보낸 호출은 그대로 둔다.
 */
@Getter
@Entity
@Table(name = "request")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Request extends BaseEntity {

    /** 요청번호 (예: AS-0001). id 를 받은 뒤 정하므로 저장 직후 채운다 */
    @Column(name = "request_no", unique = true, length = 20)
    private String requestNo;

    /** 이 호출로 이어진 AI 상담 (상담 없이 바로 호출하면 null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private Consultation consultation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    /** 호출한 사용 업체 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** 호출한 사람 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 수신할 제작 업체 (설비 모델의 제작 업체) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maker_company_id", nullable = false)
    private Company makerCompany;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "symptom_detail", nullable = false, columnDefinition = "TEXT")
    private String symptomDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_state", nullable = false, length = 20)
    private EquipmentState equipmentState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_reason", nullable = false, length = 20)
    private CallReason callReason;

    @Column(name = "reporter_name", nullable = false, length = 50)
    private String reporterName;

    @Column(name = "reporter_phone", nullable = false, length = 20)
    private String reporterPhone;

    @Column(name = "company_phone", nullable = false, length = 20)
    private String companyPhone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_summary")
    private AiSummary aiSummary;

    /** 배정된 엔지니어 (제작 업체 STAFF) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineer_id")
    private User engineer;

    @Column(name = "visit_at")
    private LocalDateTime visitAt;

    @Builder
    private Request(Consultation consultation, Equipment equipment, Company company, User user, Company makerCompany, String symptomDetail,
                    EquipmentState equipmentState, Urgency urgency, CallReason callReason, String reporterName,
                    String reporterPhone, String companyPhone, AiSummary aiSummary) {
        this.consultation = consultation;
        this.equipment = equipment;
        this.company = company;
        this.user = user;
        this.makerCompany = makerCompany;
        this.status = RequestStatus.RECEIVED;
        this.symptomDetail = symptomDetail;
        this.equipmentState = equipmentState;
        this.urgency = urgency;
        this.callReason = callReason;
        this.reporterName = reporterName;
        this.reporterPhone = reporterPhone;
        this.companyPhone = companyPhone;
        this.aiSummary = aiSummary;
    }

    public void assignRequestNo() {
        this.requestNo = "AS-%04d".formatted(getId());
    }

    /** 배정 또는 재배정 (방문 시각 변경 포함) */
    public void assign(User engineer, LocalDateTime visitAt) {
        requireOpen();
        this.engineer = engineer;
        this.visitAt = visitAt;
        this.status = RequestStatus.ASSIGNED;
    }

    public void resolveBySelf() {
        requireOpen();
        this.status = RequestStatus.SELF_RESOLVED;
    }

    /** 수리 이력 기록 → 수리 완료. 엔지니어가 배정된 호출만 가능 */
    public void complete() {
        if (status != RequestStatus.ASSIGNED) {
            throw (status.isOpen() ? ErrorCode.REQUEST_NOT_ASSIGNED : ErrorCode.INVALID_REQUEST_STATUS).toException();
        }
        this.status = RequestStatus.COMPLETED;
    }

    public boolean isRequestedBy(Long companyId) {
        return company.getId().equals(companyId);
    }

    public boolean isReceivedBy(Long makerCompanyId) {
        return makerCompany.getId().equals(makerCompanyId);
    }

    private void requireOpen() {
        if (!status.isOpen()) {
            throw ErrorCode.INVALID_REQUEST_STATUS.toException();
        }
    }
}
