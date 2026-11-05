package org.study.jibeo.domain.repairhistory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.equipment.repository.EquipmentRepository;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.domain.equipmentmodel.repository.EquipmentModelRepository;
import org.study.jibeo.domain.part.model.Part;
import org.study.jibeo.domain.part.repository.PartRepository;
import org.study.jibeo.domain.repairhistory.dto.CreateRepairHistoryRequest;
import org.study.jibeo.domain.repairhistory.dto.RepairHistoryResponse;
import org.study.jibeo.domain.repairhistory.model.RepairHistory;
import org.study.jibeo.domain.repairhistory.repository.RepairHistoryRepository;
import org.study.jibeo.domain.request.model.Request;
import org.study.jibeo.domain.request.repository.RequestRepository;
import org.study.jibeo.global.exception.ErrorCode;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RepairHistoryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RepairHistoryRepository repairHistoryRepository;
    private final RequestRepository requestRepository;
    private final PartRepository partRepository;
    private final EquipmentModelRepository modelRepository;
    private final EquipmentRepository equipmentRepository;

    /** 제작 업체: 배정된 호출에 수리 이력을 남기고 호출을 COMPLETED 로 바꾼다 */
    @Transactional
    public RepairHistoryResponse record(Long makerCompanyId, Long requestId, CreateRepairHistoryRequest body) {
        Request request = requestRepository.findDetailById(requestId)
                .filter(r -> r.isReceivedBy(makerCompanyId))
                .orElseThrow(ErrorCode.REQUEST_NOT_FOUND::toException);
        Equipment equipment = request.getEquipment();
        Part part = partRepository.findById(body.partId())
                .filter(p -> p.getEquipmentModel().getId().equals(equipment.getEquipmentModel().getId()))
                .orElseThrow(ErrorCode.PART_NOT_IN_MODEL::toException);

        request.complete();
        RepairHistory history = repairHistoryRepository.save(RepairHistory.builder()
                .part(part)
                .equipment(equipment)
                .company(request.getMakerCompany())
                .request(request)
                .cause(body.cause())
                .solution(body.solution())
                .difficulty(body.difficulty())
                .repairedAt(LocalDate.now(KST))
                .build());
        return RepairHistoryResponse.from(history);
    }

    /** 제작 업체: 우리 모델의 수리 이력 (모든 이용 기업) */
    public List<RepairHistoryResponse> getModelHistories(Long makerCompanyId, Long modelId) {
        EquipmentModel model = modelRepository.findWithMakerById(modelId)
                .filter(m -> m.isOwnedBy(makerCompanyId))
                .orElseThrow(ErrorCode.MODEL_NOT_FOUND::toException);
        return repairHistoryRepository.findAllByModelId(model.getId()).stream()
                .map(RepairHistoryResponse::from).toList();
    }

    /** 사용 업체: 우리 설비 1대의 수리 이력 */
    public List<RepairHistoryResponse> getEquipmentHistories(Long companyId, Long equipmentId) {
        Equipment equipment = equipmentRepository.findWithModelById(equipmentId)
                .filter(e -> e.isOwnedBy(companyId))
                .orElseThrow(ErrorCode.EQUIPMENT_NOT_FOUND::toException);
        return repairHistoryRepository.findAllByEquipmentId(equipment.getId()).stream()
                .map(RepairHistoryResponse::from).toList();
    }
}
