package org.study.jibeo.domain.solution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.solution.dto.DraftSolutionRequest;
import org.study.jibeo.domain.solution.dto.SolutionDetailResponse;
import org.study.jibeo.domain.solution.dto.SolutionListResponse;
import org.study.jibeo.domain.solution.dto.UpdateSolutionRequest;
import org.study.jibeo.domain.solution.model.SolutionStatus;
import org.study.jibeo.domain.solution.service.SolutionService;
import org.study.jibeo.global.security.CurrentUser;

/** 제작 업체(STAFF) 전용: 솔루션 초안 생성·검토·승인 */
@Tag(name = "솔루션 (제작 업체)")
@RestController
@RequestMapping("/api/maker")
@RequiredArgsConstructor
public class MakerSolutionController {

    private final SolutionService solutionService;

    @Operation(summary = "솔루션 목록")
    @GetMapping("/models/{modelId}/solutions")
    public SolutionListResponse getList(@PathVariable Long modelId,
                                        @RequestParam(required = false) SolutionStatus status) {
        return solutionService.getList(CurrentUser.companyId(), modelId, status);
    }

    /** 고른 수리 이력으로 AI 가 초안을 만든다 → DRAFT */
    @Operation(summary = "솔루션 초안 생성")
    @PostMapping("/models/{modelId}/solutions/draft")
    public SolutionDetailResponse draft(@PathVariable Long modelId, @Valid @RequestBody DraftSolutionRequest request) {
        return solutionService.draft(CurrentUser.companyId(), modelId, request);
    }

    @Operation(summary = "솔루션 상세")
    @GetMapping("/solutions/{solutionId}")
    public SolutionDetailResponse getDetail(@PathVariable Long solutionId) {
        return solutionService.getDetail(CurrentUser.companyId(), solutionId);
    }

    /** 증상·단계·fallback 수정 (승인된 솔루션은 승인 상태 그대로) */
    @Operation(summary = "솔루션 수정")
    @PutMapping("/solutions/{solutionId}")
    public SolutionDetailResponse update(@PathVariable Long solutionId,
                                         @Valid @RequestBody UpdateSolutionRequest request) {
        return solutionService.update(CurrentUser.companyId(), solutionId, request);
    }

    /** 승인 → AI 상담 근거로 사용 */
    @Operation(summary = "솔루션 승인")
    @PostMapping("/solutions/{solutionId}/approve")
    public SolutionDetailResponse approve(@PathVariable Long solutionId) {
        return solutionService.approve(CurrentUser.companyId(), CurrentUser.id(), solutionId);
    }

    @Operation(summary = "승인 내리기")
    @PostMapping("/solutions/{solutionId}/withdraw")
    public SolutionDetailResponse withdraw(@PathVariable Long solutionId) {
        return solutionService.withdraw(CurrentUser.companyId(), solutionId);
    }
}
