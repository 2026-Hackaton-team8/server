package org.study.jibeo.domain.consultation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.consultation.dto.AnswerRequest;
import org.study.jibeo.domain.consultation.dto.ConsultationDetailResponse;
import org.study.jibeo.domain.consultation.dto.ConsultationListResponse;
import org.study.jibeo.domain.consultation.dto.RequestDraftResponse;
import org.study.jibeo.domain.consultation.dto.StartConsultationRequest;
import org.study.jibeo.domain.consultation.model.ConsultationStatus;
import org.study.jibeo.domain.consultation.service.ConsultationService;
import org.study.jibeo.global.security.CurrentUser;

/** 사용 업체(WORKER) 전용: AI 상담 */
@Tag(name = "AI 상담 (사용 업체)")
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    /** 증상을 보내면 AI 첫 차례(질문/해결책/호출 권유)까지 담아 돌려준다 */
    @Operation(summary = "상담 시작")
    @PostMapping
    public ConsultationDetailResponse start(@Valid @RequestBody StartConsultationRequest request) {
        return consultationService.start(CurrentUser.id(), CurrentUser.companyId(), request);
    }

    /** AI 질문에 답하면 AI 다음 차례까지 담아 돌려준다 */
    @Operation(summary = "상담 답변")
    @PostMapping("/{consultationId}/messages")
    public ConsultationDetailResponse answer(@PathVariable Long consultationId,
                                             @Valid @RequestBody AnswerRequest request) {
        return consultationService.answer(CurrentUser.companyId(), consultationId, request);
    }

    @Operation(summary = "상담 내역")
    @GetMapping
    public ConsultationListResponse getList(@RequestParam(required = false) ConsultationStatus status,
                                            @RequestParam(required = false) Long equipmentId) {
        return consultationService.getList(CurrentUser.companyId(), status, equipmentId);
    }

    @Operation(summary = "상담 상세")
    @GetMapping("/{consultationId}")
    public ConsultationDetailResponse getDetail(@PathVariable Long consultationId) {
        return consultationService.getDetail(CurrentUser.companyId(), consultationId);
    }

    @Operation(summary = "해결됐어요")
    @PostMapping("/{consultationId}/resolve")
    public ConsultationDetailResponse resolve(@PathVariable Long consultationId) {
        return consultationService.resolve(CurrentUser.companyId(), consultationId);
    }

    @Operation(summary = "호출 신고서 자동 입력값")
    @GetMapping("/{consultationId}/request-draft")
    public RequestDraftResponse getRequestDraft(@PathVariable Long consultationId) {
        return consultationService.getRequestDraft(CurrentUser.id(), CurrentUser.companyId(), consultationId);
    }
}
