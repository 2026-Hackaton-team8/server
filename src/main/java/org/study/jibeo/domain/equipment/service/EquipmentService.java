package org.study.jibeo.domain.equipment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.company.repository.CompanyRepository;
import org.study.jibeo.domain.equipment.dto.CreateEquipmentRequest;
import org.study.jibeo.domain.equipment.dto.EquipmentDetailResponse;
import org.study.jibeo.domain.equipment.dto.EquipmentResponse;
import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.equipment.repository.EquipmentRepository;
import org.study.jibeo.domain.equipmentmodel.dto.PartResponse;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.domain.equipmentmodel.repository.EquipmentModelRepository;
import org.study.jibeo.domain.part.repository.PartRepository;
import org.study.jibeo.global.exception.ErrorCode;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentModelRepository modelRepository;
    private final PartRepository partRepository;
    private final CompanyRepository companyRepository;

    public List<EquipmentResponse> getMyEquipments(Long companyId) {
        return equipmentRepository.findAllWithModelByCompanyId(companyId).stream()
                .map(EquipmentResponse::from).toList();
    }

    public EquipmentDetailResponse getDetail(Long companyId, Long equipmentId) {
        Equipment equipment = findOwned(companyId, equipmentId);
        List<PartResponse> parts = partRepository
                .findAllByEquipmentModelIdOrderByCodeAsc(equipment.getEquipmentModel().getId()).stream()
                .map(PartResponse::from).toList();
        return EquipmentDetailResponse.of(equipment, parts);
    }

    @Transactional
    public EquipmentDetailResponse create(Long companyId, CreateEquipmentRequest request) {
        EquipmentModel model = modelRepository.findById(request.modelId())
                .orElseThrow(ErrorCode.MODEL_NOT_FOUND::toException);
        Equipment equipment = equipmentRepository.save(Equipment.builder()
                .company(companyRepository.getReferenceById(companyId))
                .equipmentModel(model)
                .name(request.name())
                .location(request.location())
                .serialNumber(request.serialNumber() == null || request.serialNumber().isBlank()
                        ? null : request.serialNumber())
                .build());
        return getDetail(companyId, equipment.getId());
    }

    /** 다른 회사의 설비는 존재 여부도 드러내지 않도록 404 */
    private Equipment findOwned(Long companyId, Long equipmentId) {
        Equipment equipment = equipmentRepository.findWithModelById(equipmentId)
                .orElseThrow(ErrorCode.EQUIPMENT_NOT_FOUND::toException);
        if (!equipment.isOwnedBy(companyId)) {
            throw ErrorCode.EQUIPMENT_NOT_FOUND.toException();
        }
        return equipment;
    }
}
