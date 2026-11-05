package org.study.jibeo.domain.equipmentmodel.dto;

import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;

/** 모델 검색 결과 (설비 등록 화면) */
public record ModelSummaryResponse(Long id, String code, String name, String category, String makerCompanyName) {

    public static ModelSummaryResponse from(EquipmentModel model) {
        return new ModelSummaryResponse(model.getId(), model.getCode(), model.getName(), model.getCategory(),
                model.getMakerCompany().getName());
    }
}
