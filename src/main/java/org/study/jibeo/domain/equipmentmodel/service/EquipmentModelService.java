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
import org.study.jibeo.domain.equipmentmodel.dto.ManualResponse;
import org.study.jibeo.domain.equipmentmodel.dto.ModelDetailResponse;
import org.study.jibeo.domain.equipmentmodel.dto.ModelSummaryResponse;
import org.study.jibeo.domain.equipmentmodel.dto.PartResponse;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.domain.equipmentmodel.model.Manual;
import org.study.jibeo.domain.equipmentmodel.model.ManualSection;
import org.study.jibeo.domain.equipmentmodel.repository.EquipmentModelRepository;
import org.study.jibeo.domain.equipmentmodel.repository.ManualSectionRepository;
import org.study.jibeo.domain.part.model.Part;
import org.study.jibeo.domain.part.repository.PartRepository;
import org.study.jibeo.domain.repairhistory.repository.RepairHistoryRepository;
import org.study.jibeo.domain.solution.model.SolutionStatus;
import org.study.jibeo.domain.solution.repository.SolutionRepository;
import org.study.jibeo.global.ai.AiClient;
import org.study.jibeo.global.ai.AiDtos;
import org.study.jibeo.global.exception.ErrorCode;
import team.themoment.sdk.exception.ExpectedException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentModelService {

    private final EquipmentModelRepository modelRepository;
    private final PartRepository partRepository;
    private final ManualSectionRepository sectionRepository;
    private final CompanyRepository companyRepository;
    private final EquipmentRepository equipmentRepository;
    private final RepairHistoryRepository repairHistoryRepository;
    private final SolutionRepository solutionRepository;
    private final AiClient aiClient;

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
        Map<Long, Map<SolutionStatus, Long>> solutions = new HashMap<>();
        solutionRepository.countByModelForMaker(makerCompanyId).forEach(c -> solutions
                .computeIfAbsent(c.getModelId(), k -> new EnumMap<>(SolutionStatus.class))
                .put(c.getStatus(), c.getCount()));

        return modelRepository.findAllByMakerCompanyIdOrderByCodeAsc(makerCompanyId).stream()
                .map(model -> {
                    var stat = repairs.get(model.getId());
                    return MakerModelResponse.of(model,
                            installed.getOrDefault(model.getId(), 0L),
                            stat == null ? 0 : stat.getCount(),
                            stat == null ? null : stat.getLastRepairedAt(),
                            solutions.getOrDefault(model.getId(), Map.of()).getOrDefault(SolutionStatus.APPROVED, 0L),
                            solutions.getOrDefault(model.getId(), Map.of()).getOrDefault(SolutionStatus.DRAFT, 0L));
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

    public ManualResponse getManual(Long makerCompanyId, Long modelId) {
        EquipmentModel model = findOwnedModel(makerCompanyId, modelId);
        return ManualResponse.of(model.getManualText(), model.getManual(),
                sectionRepository.findAllByEquipmentModelIdOrderBySortOrderAsc(modelId));
    }

    /**
     * 전문을 통째로 바꾼다. 제목·줄 머리표로 나눈 항목을 manual_section 에 다시 저장하고,
     * AI 가 읽는 jsonb(equipment_model.manual)도 같은 항목으로 채운 뒤 AI 에 다시 색인하게 한다
     */
    @Transactional
    public ManualResponse updateManual(Long makerCompanyId, Long modelId, String content) {
        EquipmentModel model = findOwnedModel(makerCompanyId, modelId);
        List<ManualParser.Section> parsed;
        try {
            parsed = ManualParser.parse(content.strip(), model.getName() + " 매뉴얼");
        } catch (IllegalArgumentException e) {
            throw new ExpectedException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        int version = model.getManual() == null ? 1 : model.getManual().version() + 1;
        Manual manual = new Manual(version, parsed.stream().map(ManualParser.Section::toAi).toList());
        model.updateManual(content, manual);

        sectionRepository.deleteAllByModelId(modelId);
        List<ManualSection> sections = new ArrayList<>();
        for (int i = 0; i < parsed.size(); i++) {
            ManualParser.Section s = parsed.get(i);
            sections.add(ManualSection.builder()
                    .equipmentModel(model).sectionKey(s.key()).sortOrder(i).type(s.type()).title(s.title())
                    .errorCodes(s.errorCodes()).symptoms(s.symptoms()).causes(s.causes()).steps(s.steps())
                    .escalation(s.escalation()).cautions(s.cautions()).source(s.source())
                    .description(s.description()).content(s.content())
                    .build());
        }
        sectionRepository.saveAll(sections);

        aiClient.notifyIndex(AiDtos.IndexKind.MANUAL, model.getId());
        return ManualResponse.of(content, manual, sections);
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
}
