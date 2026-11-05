package org.study.jibeo.domain.solution.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;
import org.study.jibeo.domain.equipmentmodel.repository.EquipmentModelRepository;
import org.study.jibeo.domain.repairhistory.model.RepairHistory;
import org.study.jibeo.domain.repairhistory.repository.RepairHistoryRepository;
import org.study.jibeo.domain.solution.dto.DraftSolutionRequest;
import org.study.jibeo.domain.solution.dto.SolutionDetailResponse;
import org.study.jibeo.domain.solution.dto.SolutionListResponse;
import org.study.jibeo.domain.solution.dto.UpdateSolutionRequest;
import org.study.jibeo.domain.solution.model.Solution;
import org.study.jibeo.domain.solution.model.SolutionStatus;
import org.study.jibeo.domain.solution.repository.SolutionRepository;
import org.study.jibeo.domain.user.repository.UserRepository;
import org.study.jibeo.global.ai.AiClient;
import org.study.jibeo.global.ai.AiDtos;
import org.study.jibeo.global.exception.ErrorCode;
import team.themoment.sdk.exception.ExpectedException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 솔루션 초안: 제작 업체가 고른 수리 이력으로 AI 가 초안을 쓰고, 검토·수정 후 승인하면 AI 상담 근거가 된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolutionService {

    private static final int MAX_STEPS = 20;

    private final SolutionRepository solutionRepository;
    private final EquipmentModelRepository modelRepository;
    private final RepairHistoryRepository repairHistoryRepository;
    private final UserRepository userRepository;
    private final AiClient aiClient;

    public SolutionListResponse getList(Long makerCompanyId, Long modelId, SolutionStatus status) {
        EquipmentModel model = findOwnedModel(makerCompanyId, modelId);
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ALL", 0L);
        for (SolutionStatus s : SolutionStatus.values()) {
            counts.put(s.name(), 0L);
        }
        for (SolutionRepository.StatusCount c : solutionRepository.countByStatus(model.getId())) {
            counts.put(c.getStatus().name(), c.getCount());
            counts.merge("ALL", c.getCount(), Long::sum);
        }
        return new SolutionListResponse(counts, solutionRepository.findList(model.getId(), status).stream()
                .map(SolutionListResponse.Item::from).toList());
    }

    @Transactional
    public SolutionDetailResponse draft(Long makerCompanyId, Long modelId, DraftSolutionRequest body) {
        EquipmentModel model = findOwnedModel(makerCompanyId, modelId);
        Set<Long> ids = new LinkedHashSet<>(body.repairHistoryIds());
        List<RepairHistory> histories = repairHistoryRepository.findAllById(ids);
        if (histories.size() != ids.size()
                || histories.stream().anyMatch(h -> !h.getEquipment().getEquipmentModel().getId().equals(modelId))) {
            throw ErrorCode.REPAIR_HISTORY_NOT_IN_MODEL.toException();
        }

        AiDtos.DraftResponse res = aiClient.draftSolution(new AiDtos.DraftRequest(
                new AiDtos.ModelInfo(model.getId(), model.getCode(), model.getName(), model.getCategory()),
                histories.stream().map(h -> new AiDtos.History(h.getId(),
                        new AiDtos.PartInfo(h.getPart().getCode(), h.getPart().getName()),
                        h.getRequest().getSymptomDetail(), h.getCause(), h.getSolution(),
                        h.getDifficulty().name(), h.getRepairedAt())).toList()));
        List<String> steps = res == null || res.steps() == null ? List.of()
                : res.steps().stream().filter(s -> s != null && !s.isBlank())
                .limit(MAX_STEPS).map(s -> cut(s, 500)).toList();
        if (steps.isEmpty()) {
            throw new ExpectedException("AI가 초안을 만들지 못했습니다. 다른 수리 이력을 골라 다시 시도해 주세요.",
                    HttpStatus.BAD_GATEWAY);
        }

        // AI 가 근거로 쓴 이력 중 요청한 것만 남기고, 없으면 요청한 이력 전부를 근거로 둔다
        List<RepairHistory> evidence = res.evidenceRepairHistoryIds() == null ? List.of()
                : histories.stream().filter(h -> res.evidenceRepairHistoryIds().contains(h.getId())).toList();
        Solution solution = solutionRepository.save(Solution.builder()
                .equipmentModel(model)
                .title(cut(orDefault(res.title(), histories.getFirst().getRequest().getSymptomDetail()), 200))
                .category(cut(orDefault(res.category(), model.getCategory()), 50))
                .symptoms(res.symptoms() == null ? List.of()
                        : res.symptoms().stream().filter(s -> s != null && !s.isBlank()).map(s -> cut(s, 100))
                        .limit(20).toList())
                .steps(steps)
                .fallback(res.fallback() == null ? null : cut(res.fallback(), 2000))
                .evidences(evidence.isEmpty() ? histories : evidence)
                .build());
        return SolutionDetailResponse.from(solution);
    }

    public SolutionDetailResponse getDetail(Long makerCompanyId, Long solutionId) {
        return SolutionDetailResponse.from(findOwned(makerCompanyId, solutionId));
    }

    @Transactional
    public SolutionDetailResponse update(Long makerCompanyId, Long solutionId, UpdateSolutionRequest body) {
        Solution solution = findOwned(makerCompanyId, solutionId);
        solution.update(body.title(), body.category(), body.symptoms(), body.steps(), body.fallback());
        if (solution.getStatus() == SolutionStatus.APPROVED) {
            aiClient.notifyIndex(AiDtos.IndexKind.SOLUTION, solution.getId());
        }
        return SolutionDetailResponse.from(solution);
    }

    @Transactional
    public SolutionDetailResponse approve(Long makerCompanyId, Long userId, Long solutionId) {
        Solution solution = findOwned(makerCompanyId, solutionId);
        solution.approve(userRepository.getReferenceById(userId));
        aiClient.notifyIndex(AiDtos.IndexKind.SOLUTION, solution.getId());
        return SolutionDetailResponse.from(solution);
    }

    @Transactional
    public SolutionDetailResponse withdraw(Long makerCompanyId, Long solutionId) {
        Solution solution = findOwned(makerCompanyId, solutionId);
        solution.withdraw();
        aiClient.notifyIndex(AiDtos.IndexKind.SOLUTION, solution.getId());
        return SolutionDetailResponse.from(solution);
    }

    /** 다른 제작 업체의 솔루션은 존재 여부도 드러내지 않도록 404 */
    private Solution findOwned(Long makerCompanyId, Long solutionId) {
        return solutionRepository.findDetailById(solutionId)
                .filter(s -> s.isOwnedBy(makerCompanyId))
                .orElseThrow(ErrorCode.SOLUTION_NOT_FOUND::toException);
    }

    private EquipmentModel findOwnedModel(Long makerCompanyId, Long modelId) {
        return modelRepository.findWithMakerById(modelId)
                .filter(m -> m.isOwnedBy(makerCompanyId))
                .orElseThrow(ErrorCode.MODEL_NOT_FOUND::toException);
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
