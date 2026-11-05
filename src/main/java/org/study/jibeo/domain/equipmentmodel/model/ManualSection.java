package org.study.jibeo.domain.equipmentmodel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.study.jibeo.global.entity.BaseEntity;

import java.util.List;

/**
 * 매뉴얼 항목. 모델의 매뉴얼 전문을 제목 기준으로 나누고 줄 머리표(증상·원인·조치 …)를 칸으로 저장한다.
 * 매뉴얼을 저장할 때마다 모델의 항목을 모두 지우고 다시 만든다.
 */
@Getter
@Entity
@Table(name = "manual_section",
        uniqueConstraints = @UniqueConstraint(columnNames = {"equipment_model_id", "section_key"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManualSection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_model_id", nullable = false)
    private EquipmentModel equipmentModel;

    /** 매뉴얼 안에서의 항목 id (s-001 …). AI 근거(evidence.manualSectionIds)와 같은 값 */
    @Column(name = "section_key", nullable = false, length = 64)
    private String sectionKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Manual.SectionType type;

    @Column(nullable = false, length = 200)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_codes")
    private List<String> errorCodes;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> symptoms;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> causes;

    /** 작업자가 직접 해도 되는 조치 단계 */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> steps;

    /** 담당자를 불러야 하는 기준 */
    @Column(columnDefinition = "TEXT")
    private String escalation;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> cautions;

    /** 근거 문서 (제조사 매뉴얼 등) */
    @Column(columnDefinition = "TEXT")
    private String source;

    /** 줄 머리표 밖의 설명 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 제목 아래 원문 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    private ManualSection(EquipmentModel equipmentModel, String sectionKey, int sortOrder, Manual.SectionType type,
                          String title, List<String> errorCodes, List<String> symptoms, List<String> causes,
                          List<String> steps, String escalation, List<String> cautions, String source,
                          String description, String content) {
        this.equipmentModel = equipmentModel;
        this.sectionKey = sectionKey;
        this.sortOrder = sortOrder;
        this.type = type;
        this.title = title;
        this.errorCodes = errorCodes;
        this.symptoms = symptoms;
        this.causes = causes;
        this.steps = steps;
        this.escalation = escalation;
        this.cautions = cautions;
        this.source = source;
        this.description = description;
        this.content = content;
    }
}
