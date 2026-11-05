package org.study.jibeo.domain.equipmentmodel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 매뉴얼 전문은 모델 등록 뒤 PUT /api/maker/models/{id}/manual 로 입력한다 */
public record CreateModelRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String category,
        @Size(max = 255) String modelFileKey
) {
}
