package org.study.jibeo.domain.request.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.study.jibeo.domain.request.model.AiSummary;
import org.study.jibeo.domain.request.model.EquipmentState;
import org.study.jibeo.domain.request.model.Urgency;
import org.study.jibeo.global.validation.Phone;

import java.util.List;

/**
 * 호출 신고서 제출. contact 는 내 정보·회사 정보로 미리 채워 보내고, 신고서에서 바꿀 수 있다.
 * attachmentKeys 는 POST /api/files 응답의 fileKey (최대 5개, 선택).
 * TODO 상담(consultationId)은 AI 상담 구현 때 추가
 */
public record CreateRequestRequest(
        @NotNull Long equipmentId,
        @Valid AiSummary aiSummary,
        @NotBlank @Size(max = 2000) String symptomDetail,
        @NotNull EquipmentState equipmentState,
        @NotNull Urgency urgency,
        @NotNull @Valid Contact contact,
        @Size(max = 5) List<@NotBlank String> attachmentKeys
) {

    public record Contact(
            @NotBlank @Size(max = 50) String reporterName,
            @NotBlank @Phone String reporterPhone,
            @NotBlank @Phone String companyPhone
    ) {
    }
}
