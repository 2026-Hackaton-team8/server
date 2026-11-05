package org.study.jibeo.domain.equipment.model;

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
import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.global.entity.BaseEntity;

/**
 * 사용 업체가 등록한 설치 설비 1대 (예: 1층 세탁실 1호기)
 */
@Getter
@Entity
@Table(name = "equipment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_model_id", nullable = false)
    private EquipmentModel equipmentModel;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(name = "serial_number", length = 50)
    private String serialNumber;

    @Builder
    private Equipment(Company company, EquipmentModel equipmentModel, String name, String location, String serialNumber) {
        this.company = company;
        this.equipmentModel = equipmentModel;
        this.name = name;
        this.location = location;
        this.serialNumber = serialNumber;
    }

    public boolean isOwnedBy(Long companyId) {
        return company.getId().equals(companyId);
    }
}
