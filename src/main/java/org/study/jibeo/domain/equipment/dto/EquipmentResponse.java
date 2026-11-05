package org.study.jibeo.domain.equipment.dto;

import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;

/** 내 설비 목록 */
public record EquipmentResponse(Long id, String name, String location, String serialNumber, ModelInfo model) {

    public record ModelInfo(Long id, String code, String name, String category) {

        static ModelInfo from(EquipmentModel model) {
            return new ModelInfo(model.getId(), model.getCode(), model.getName(), model.getCategory());
        }
    }

    public static EquipmentResponse from(Equipment equipment) {
        return new EquipmentResponse(equipment.getId(), equipment.getName(), equipment.getLocation(),
                equipment.getSerialNumber(), ModelInfo.from(equipment.getEquipmentModel()));
    }
}
