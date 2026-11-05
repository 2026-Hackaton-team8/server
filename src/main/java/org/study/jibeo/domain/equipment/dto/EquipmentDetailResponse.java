package org.study.jibeo.domain.equipment.dto;

import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.equipmentmodel.dto.PartResponse;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;

import java.util.List;

/** 설비 상세 (상담 시작 화면): 모델 정보와 부품 목록 포함 */
public record EquipmentDetailResponse(
        Long id,
        String name,
        String location,
        String serialNumber,
        ModelInfo model,
        List<PartResponse> parts
) {

    public record ModelInfo(Long id, String code, String name, String category, String makerCompanyName,
                            String modelFileKey) {
    }

    public static EquipmentDetailResponse of(Equipment equipment, List<PartResponse> parts) {
        EquipmentModel m = equipment.getEquipmentModel();
        return new EquipmentDetailResponse(equipment.getId(), equipment.getName(), equipment.getLocation(),
                equipment.getSerialNumber(),
                new ModelInfo(m.getId(), m.getCode(), m.getName(), m.getCategory(), m.getMakerCompany().getName(),
                        m.getModelFileKey()),
                parts);
    }
}
