package org.study.jibeo.domain.equipmentmodel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 매뉴얼 전문. 마크다운 제목(#, ## …)마다 AI 검색 항목으로 나뉜다 */
public record UpdateManualRequest(
        @NotBlank @Size(max = 1_000_000) String content
) {
}
