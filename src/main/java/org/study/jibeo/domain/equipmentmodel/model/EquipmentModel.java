package org.study.jibeo.domain.equipmentmodel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.global.entity.BaseEntity;

/**
 * 제작 업체가 등록한 설비 모델 (예: WM-20 산업용 세탁기 20kg)
 */
@Getter
@Entity
@Table(name = "equipment_model",
        uniqueConstraints = @UniqueConstraint(columnNames = {"maker_company_id", "code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentModel extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maker_company_id", nullable = false)
    private Company makerCompany;

    @Column(name = "model_file_key", length = 255)
    private String modelFileKey;

    /** 제작 업체가 입력한 매뉴얼 전문 (마크다운) */
    @Column(name = "manual_text", columnDefinition = "text")
    private String manualText;

    /**
     * 전문을 제목 기준으로 나눈 AI 검색 항목 (jsonb). Python AI 가 이 컬럼을 직접 읽어 색인하므로 형식을 유지한다.
     * 전문 없이 항목만 있는 예전 데이터도 있다
     */
    @JdbcTypeCode(SqlTypes.JSON)
    private Manual manual;

    @Builder
    private EquipmentModel(String code, String name, String category, Company makerCompany, String modelFileKey) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.makerCompany = makerCompany;
        this.modelFileKey = modelFileKey;
    }

    public void updateManual(String manualText, Manual manual) {
        this.manualText = manualText;
        this.manual = manual;
    }

    public boolean isOwnedBy(Long companyId) {
        return makerCompany.getId().equals(companyId);
    }
}
