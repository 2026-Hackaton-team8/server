package org.study.jibeo.domain.equipmentmodel.dto;

import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;

/** 선택한 모델 정보 (설비 등록 화면 오른쪽 패널) */
public record ModelDetailResponse(
        Long id,
        String code,
        String name,
        String category,
        Long makerCompanyId,
        String makerCompanyName,
        String modelFileKey,
        long partCount,
        long repairHistoryCount
) {

    public static ModelDetailResponse of(EquipmentModel model, long partCount, long repairHistoryCount) {
        return new ModelDetailResponse(model.getId(), model.getCode(), model.getName(), model.getCategory(),
                model.getMakerCompany().getId(), model.getMakerCompany().getName(), model.getModelFileKey(),
                partCount, repairHistoryCount);
    }
}
