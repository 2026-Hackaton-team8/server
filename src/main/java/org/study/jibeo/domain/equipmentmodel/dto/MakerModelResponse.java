package org.study.jibeo.domain.equipmentmodel.dto;

import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;

import java.time.LocalDate;

/** 제작 업체 모델 목록 (솔루션 초안 · 모델 목록 화면). 솔루션 수는 솔루션 기능 구현 때 추가 */
public record MakerModelResponse(
        Long id,
        String code,
        String name,
        String category,
        long installedCount,
        long repairHistoryCount,
        LocalDate lastRepairedAt
) {

    public static MakerModelResponse of(EquipmentModel model, long installedCount, long repairHistoryCount,
                                        LocalDate lastRepairedAt) {
        return new MakerModelResponse(model.getId(), model.getCode(), model.getName(), model.getCategory(),
                installedCount, repairHistoryCount, lastRepairedAt);
    }
}
