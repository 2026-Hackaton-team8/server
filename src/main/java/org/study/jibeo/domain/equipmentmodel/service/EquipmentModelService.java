package org.study.jibeo.domain.equipmentmodel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.domain.company.repository.CompanyRepository;
import org.study.jibeo.domain.equipment.repository.EquipmentRepository;
import org.study.jibeo.domain.equipmentmodel.dto.CreateModelRequest;
import org.study.jibeo.domain.equipmentmodel.dto.CreatePartRequest;
import org.study.jibeo.domain.equipmentmodel.dto.MakerModelResponse;
import org.study.jibeo.domain.equipmentmodel.dto.ModelDetailResponse;
import org.study.jibeo.domain.equipmentmodel.dto.ModelSummaryResponse;
import org.study.jibeo.domain.equipmentmodel.dto.PartResponse;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.domain.equipmentmodel.model.Manual;
import org.study.jibeo.domain.equipmentmodel.repository.EquipmentModelRepository;
import org.study.jibeo.domain.part.model.Part;
import org.study.jibeo.domain.part.repository.PartRepository;
import org.study.jibeo.domain.repairhistory.repository.RepairHistoryRepository;
import org.study.jibeo.global.exception.ErrorCode;
import team.themoment.sdk.exception.ExpectedException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentModelService {

    private final EquipmentModelRepository modelRepository;
    private final PartRepository partRepository;
    private final CompanyRepository companyRepository;
    private final EquipmentRepository equipmentRepository;
    private final RepairHistoryRepository repairHistoryRepository;

    // ---------- 공통 (사용 업체·제작 업체) ----------

    public List<ModelSummaryResponse> search(String q) {
        String keyword = q == null || q.isBlank() ? null : q.trim();
        return modelRepository.search(keyword).stream().map(ModelSummaryResponse::from).toList();
    }

    public ModelDetailResponse getDetail(Long modelId) {
        EquipmentModel model = modelRepository.findWithMakerById(modelId)
                .orElseThrow(ErrorCode.MODEL_NOT_FOUND::toException);
        return ModelDetailResponse.of(model,
                partRepository.countByEquipmentModelId(modelId),
                repairHistoryRepository.countByEquipmentEquipmentModelId(modelId));
    }

    public List<PartResponse> getParts(Long modelId) {
        if (!modelRepository.existsById(modelId)) {
            throw ErrorCode.MODEL_NOT_FOUND.toException();
        }
        return partRepository.findAllByEquipmentModelIdOrderByCodeAsc(modelId).stream()
                .map(PartResponse::from).toList();
    }

    // ---------- 제작 업체 ----------

    public List<MakerModelResponse> getMakerModels(Long makerCompanyId) {
        Map<Long, Long> installed = equipmentRepository.countByModelForMaker(makerCompanyId).stream()
                .collect(Collectors.toMap(EquipmentRepository.ModelCount::getModelId,
                        EquipmentRepository.ModelCount::getCount));
        Map<Long, RepairHistoryRepository.ModelRepairStat> repairs =
                repairHistoryRepository.statByModelForMaker(makerCompanyId).stream()
                        .collect(Collectors.toMap(RepairHistoryRepository.ModelRepairStat::getModelId,
                                Function.identity()));

        return modelRepository.findAllByMakerCompanyIdOrderByCodeAsc(makerCompanyId).stream()
                .map(model -> {
                    var stat = repairs.get(model.getId());
                    return MakerModelResponse.of(model,
                            installed.getOrDefault(model.getId(), 0L),
                            stat == null ? 0 : stat.getCount(),
                            stat == null ? null : stat.getLastRepairedAt());
                })
                .toList();
    }

    @Transactional
    public ModelDetailResponse createModel(Long makerCompanyId, CreateModelRequest request) {
        String code = request.code().trim();
        if (modelRepository.existsByMakerCompanyIdAndCode(makerCompanyId, code)) {
            throw ErrorCode.DUPLICATE_MODEL_CODE.toException();
        }
        Company maker = companyRepository.getReferenceById(makerCompanyId);
        EquipmentModel model = modelRepository.save(EquipmentModel.builder()
                .code(code)
                .name(request.name())
                .category(request.category())
                .makerCompany(maker)
                .modelFileKey(request.modelFileKey())
                .build());
        return getDetail(model.getId());
    }

    @Transactional
    public PartResponse createPart(Long makerCompanyId, Long modelId, CreatePartRequest request) {
        EquipmentModel model = findOwnedModel(makerCompanyId, modelId);
        String code = request.code().trim();
        if (partRepository.existsByEquipmentModelIdAndCode(modelId, code)) {
            throw ErrorCode.DUPLICATE_PART_CODE.toException();
        }
        Part part = partRepository.save(Part.builder()
                .equipmentModel(model)
                .code(code)
                .name(request.name())
                .locationDesc(request.locationDesc())
                .meshName(request.meshName())
                .build());
        return PartResponse.from(part);
    }

    public Manual getManual(Long makerCompanyId, Long modelId) {
        Manual manual = findOwnedModel(makerCompanyId, modelId).getManual();
        return manual != null ? manual : new Manual(1, List.of());
    }

    @Transactional
    public Manual updateManual(Long makerCompanyId, Long modelId, Manual manual) {
        EquipmentModel model = findOwnedModel(makerCompanyId, modelId);
        validateManual(modelId, manual);
        model.updateManual(manual);
        return manual;
    }

    /** 다른 제작 업체의 모델은 존재 여부도 드러내지 않도록 404 */
    private EquipmentModel findOwnedModel(Long makerCompanyId, Long modelId) {
        EquipmentModel model = modelRepository.findWithMakerById(modelId)
                .orElseThrow(ErrorCode.MODEL_NOT_FOUND::toException);
        if (!model.isOwnedBy(makerCompanyId)) {
            throw ErrorCode.MODEL_NOT_FOUND.toException();
        }
        return model;
    }

    /** 섹션 id 중복과, partCodes 가 이 모델에 등록된 부품인지 검사 */
    private void validateManual(Long modelId, Manual manual) {
        Set<String> ids = new HashSet<>();
        for (Manual.Section section : manual.sections()) {
            if (!ids.add(section.id())) {
                throw badManual("섹션 id가 중복됩니다: " + section.id());
            }
        }
        Set<String> partCodes = partRepository.findAllByEquipmentModelIdOrderByCodeAsc(modelId).stream()
                .map(Part::getCode)
                .collect(Collectors.toSet());
        for (Manual.Section section : manual.sections()) {
            if (section.partCodes() == null) {
                continue;
            }
            for (String code : section.partCodes()) {
                if (!partCodes.contains(code)) {
                    throw badManual("섹션 '" + section.id() + "'의 부품 코드 " + code + "가 이 모델에 등록되어 있지 않습니다.");
                }
            }
        }
    }

    private static ExpectedException badManual(String message) {
        return new ExpectedException(message, HttpStatus.BAD_REQUEST);
    }
}
