package org.study.jibeo.domain.request.dto;

import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.domain.request.model.CallReason;
import org.study.jibeo.domain.request.model.Request;
import org.study.jibeo.domain.request.model.RequestStatus;
import org.study.jibeo.domain.request.model.Urgency;

import java.time.LocalDateTime;

/** 호출 목록 한 줄 (사용 업체 호출 목록, 제작 업체 호출 수신함 공용) */
public record RequestSummaryResponse(
        Long id,
        String requestNo,
        RequestStatus status,
        Urgency urgency,
        CallReason callReason,
        String companyName,
        String equipmentName,
        String modelCode,
        String modelName,
        String symptomSummary,
        LocalDateTime createdAt
) {

    private static final int SUMMARY_LENGTH = 60;

    public static RequestSummaryResponse from(Request r) {
        Equipment e = r.getEquipment();
        EquipmentModel m = e.getEquipmentModel();
        return new RequestSummaryResponse(r.getId(), r.getRequestNo(), r.getStatus(), r.getUrgency(),
                r.getCallReason(), r.getCompany().getName(), e.getName(), m.getCode(), m.getName(),
                summarize(r), r.getCreatedAt());
    }

    /** AI 요약의 증상이 있으면 그것을, 없으면 증상 자세히의 앞부분 */
    private static String summarize(Request r) {
        if (r.getAiSummary() != null && r.getAiSummary().symptom() != null && !r.getAiSummary().symptom().isBlank()) {
            return r.getAiSummary().symptom();
        }
        String detail = r.getSymptomDetail();
        return detail.length() <= SUMMARY_LENGTH ? detail : detail.substring(0, SUMMARY_LENGTH) + "…";
    }
}
