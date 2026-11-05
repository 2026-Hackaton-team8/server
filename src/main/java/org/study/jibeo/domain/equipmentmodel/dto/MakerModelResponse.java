package org.study.jibeo.domain.equipmentmodel.dto;

import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;

import java.time.LocalDate;

/** 제작 업체 모델 목록 (솔루션 초안 · 모델 목록 화면) */
public record MakerModelResponse(
        Long id,
        String code,
        String name,
        String category,
        long installedCount,
        long repairHistoryCount,
        LocalDate lastRepairedAt,
        long approvedSolutionCount,
        long draftSolutionCount
) {

    public static MakerModelResponse of(EquipmentModel model, long installedCount, long repairHistoryCount,
                                        LocalDate lastRepairedAt, long approvedSolutionCount,
                                        long draftSolutionCount) {
        return new MakerModelResponse(model.getId(), model.getCode(), model.getName(), model.getCategory(),
                installedCount, repairHistoryCount, lastRepairedAt, approvedSolutionCount, draftSolutionCount);
    }
}
