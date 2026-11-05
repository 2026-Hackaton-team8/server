package org.study.jibeo.domain.request.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.study.jibeo.domain.request.model.AiSummary;
import org.study.jibeo.domain.request.model.EquipmentState;
import org.study.jibeo.domain.request.model.Urgency;
import org.study.jibeo.global.validation.Phone;

/**
 * 호출 신고서 제출. contact 는 내 정보·회사 정보로 미리 채워 보내고, 신고서에서 바꿀 수 있다.
 * TODO 상담(consultationId)·첨부 파일(attachmentKeys)은 해당 기능 구현 때 추가
 */
public record CreateRequestRequest(
        @NotNull Long equipmentId,
        @Valid AiSummary aiSummary,
        @NotBlank @Size(max = 2000) String symptomDetail,
        @NotNull EquipmentState equipmentState,
        @NotNull Urgency urgency,
        @NotNull @Valid Contact contact
) {

    public record Contact(
            @NotBlank @Size(max = 50) String reporterName,
            @NotBlank @Phone String reporterPhone,
            @NotBlank @Phone String companyPhone
    ) {
    }
}
