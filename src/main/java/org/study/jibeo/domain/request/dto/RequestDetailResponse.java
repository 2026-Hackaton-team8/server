package org.study.jibeo.domain.request.dto;

import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.domain.request.model.AiSummary;
import org.study.jibeo.domain.request.model.CallReason;
import org.study.jibeo.domain.request.model.EquipmentState;
import org.study.jibeo.domain.request.model.Request;
import org.study.jibeo.domain.request.model.RequestStatus;
import org.study.jibeo.domain.request.model.Urgency;
import org.study.jibeo.domain.user.model.User;

import java.time.LocalDateTime;

/** 호출 상세 (사용 업체 진행 상태, 제작 업체 호출 상세 공용) */
public record RequestDetailResponse(
        Long id,
        String requestNo,
        RequestStatus status,
        Urgency urgency,
        EquipmentState equipmentState,
        CallReason callReason,
        String symptomDetail,
        AiSummary aiSummary,
        EquipmentInfo equipment,
        CompanyInfo company,
        Contact contact,
        EngineerInfo engineer,
        LocalDateTime visitAt,
        LocalDateTime createdAt
) {

    public record EquipmentInfo(Long id, String name, String location, String serialNumber,
                                Long modelId, String modelCode, String modelName) {
    }

    public record CompanyInfo(Long id, String name) {
    }

    public record Contact(String reporterName, String reporterPhone, String companyPhone) {
    }

    public record EngineerInfo(Long id, String name, String phone) {
    }

    public static RequestDetailResponse from(Request r) {
        Equipment e = r.getEquipment();
        EquipmentModel m = e.getEquipmentModel();
        User engineer = r.getEngineer();
        return new RequestDetailResponse(r.getId(), r.getRequestNo(), r.getStatus(), r.getUrgency(),
                r.getEquipmentState(), r.getCallReason(), r.getSymptomDetail(), r.getAiSummary(),
                new EquipmentInfo(e.getId(), e.getName(), e.getLocation(), e.getSerialNumber(),
                        m.getId(), m.getCode(), m.getName()),
                new CompanyInfo(r.getCompany().getId(), r.getCompany().getName()),
                new Contact(r.getReporterName(), r.getReporterPhone(), r.getCompanyPhone()),
                engineer == null ? null : new EngineerInfo(engineer.getId(), engineer.getName(), engineer.getPhone()),
                r.getVisitAt(), r.getCreatedAt());
    }
}
