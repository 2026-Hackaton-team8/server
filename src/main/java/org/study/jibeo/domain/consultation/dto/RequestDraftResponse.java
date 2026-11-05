package org.study.jibeo.domain.consultation.dto;

import org.study.jibeo.domain.equipmentmodel.dto.PartResponse;
import org.study.jibeo.domain.request.model.AiSummary;
import org.study.jibeo.domain.request.model.CallReason;

/**
 * 호출 신고서 자동 입력값. 그대로(또는 고쳐서) POST /api/requests 에 consultationId 와 함께 보낸다.
 * callReason 은 참고용이며 실제 값은 제출 때 서버가 정한다.
 */
public record RequestDraftResponse(
        Long consultationId,
        Long equipmentId,
        String equipmentName,
        String modelCode,
        String modelName,
        AiSummary aiSummary,
        CallReason callReason,
        PartResponse suspectedPart,
        Contact contact
) {

    public record Contact(String reporterName, String reporterPhone, String companyPhone) {
    }
}
