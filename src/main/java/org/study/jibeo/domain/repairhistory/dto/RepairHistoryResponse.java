package org.study.jibeo.domain.repairhistory.dto;

import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.part.model.Part;
import org.study.jibeo.domain.repairhistory.model.RepairDifficulty;
import org.study.jibeo.domain.repairhistory.model.RepairHistory;
import org.study.jibeo.domain.request.model.Request;
import org.study.jibeo.domain.user.model.User;

import java.time.LocalDate;

public record RepairHistoryResponse(
        Long id,
        Long requestId,
        String requestNo,
        EquipmentInfo equipment,
        PartInfo part,
        String cause,
        String solution,
        RepairDifficulty difficulty,
        String engineerName,
        LocalDate repairedAt
) {

    /** companyName = 설비를 쓰는 이용 기업 */
    public record EquipmentInfo(Long id, String name, String companyName) {
    }

    public record PartInfo(Long id, String code, String name) {
    }

    public static RepairHistoryResponse from(RepairHistory h) {
        Equipment e = h.getEquipment();
        Part p = h.getPart();
        Request r = h.getRequest();
        User engineer = r.getEngineer();
        return new RepairHistoryResponse(h.getId(), r.getId(), r.getRequestNo(),
                new EquipmentInfo(e.getId(), e.getName(), e.getCompany().getName()),
                new PartInfo(p.getId(), p.getCode(), p.getName()),
                h.getCause(), h.getSolution(), h.getDifficulty(),
                engineer == null ? null : engineer.getName(), h.getRepairedAt());
    }
}
