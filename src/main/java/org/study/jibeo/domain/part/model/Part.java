package org.study.jibeo.domain.part.model;

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
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.global.entity.BaseEntity;

/**
 * 모델의 부품 (예: P-07 배수 필터)
 */
@Getter
@Entity
@Table(name = "part",
        uniqueConstraints = @UniqueConstraint(columnNames = {"equipment_model_id", "code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Part extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_model_id", nullable = false)
    private EquipmentModel equipmentModel;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "location_desc", length = 255)
    private String locationDesc;

    /** 3D 파일 안의 부품 노드 이름 (클릭·강조용) */
    @Column(name = "mesh_name", length = 100)
    private String meshName;

    @Builder
    private Part(EquipmentModel equipmentModel, String code, String name, String locationDesc, String meshName) {
        this.equipmentModel = equipmentModel;
        this.code = code;
        this.name = name;
        this.locationDesc = locationDesc;
        this.meshName = meshName;
    }
}
