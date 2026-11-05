package org.study.jibeo.domain.request.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.consultation.dto.MessageResponse;
import org.study.jibeo.domain.request.dto.AssignRequest;
import org.study.jibeo.domain.request.dto.EngineerResponse;
import org.study.jibeo.domain.request.dto.InboxResponse;
import org.study.jibeo.domain.request.dto.RequestDetailResponse;
import org.study.jibeo.domain.request.model.RequestStatus;
import org.study.jibeo.domain.request.service.RequestService;
import org.study.jibeo.global.security.CurrentUser;

import java.util.List;

/** 제작 업체(STAFF) 전용: 호출 수신함·호출 상세·엔지니어 배정 */
@Validated
@Tag(name = "호출 수신함 (제작 업체)")
@RestController
@RequestMapping("/api/maker")
@RequiredArgsConstructor
public class MakerRequestController {

    private final RequestService requestService;

    /** 호출 수신함 + 탭별 개수 */
    @Operation(summary = "호출 수신함")
    @GetMapping("/requests")
    public InboxResponse getInbox(@RequestParam(required = false) RequestStatus status,
                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return requestService.getInbox(CurrentUser.companyId(), status, page, size);
    }

    @Operation(summary = "호출 상세")
    @GetMapping("/requests/{requestId}")
    public RequestDetailResponse getRequest(@PathVariable Long requestId) {
        return requestService.getReceived(CurrentUser.companyId(), requestId);
    }

    /** 호출로 이어진 AI 상담 대화 전체 (상담 없이 호출했으면 빈 목록) */
    @Operation(summary = "대화 전체 보기")
    @GetMapping("/requests/{requestId}/conversation")
    public List<MessageResponse> getConversation(@PathVariable Long requestId) {
        return requestService.getConversation(CurrentUser.companyId(), requestId);
    }

    /** 엔지니어 배정·재배정 → ASSIGNED */
    @Operation(summary = "엔지니어 배정")
    @PostMapping("/requests/{requestId}/assign")
    public RequestDetailResponse assign(@PathVariable Long requestId, @Valid @RequestBody AssignRequest request) {
        return requestService.assign(CurrentUser.companyId(), requestId, request);
    }

    /** 자가 해결로 종료 → SELF_RESOLVED */
    @Operation(summary = "자가 해결 처리")
    @PostMapping("/requests/{requestId}/self-resolved")
    public RequestDetailResponse resolveBySelf(@PathVariable Long requestId) {
        return requestService.resolveBySelf(CurrentUser.companyId(), requestId);
    }

    /** 배정할 수 있는 엔지니어 (우리 회사 STAFF) */
    @Operation(summary = "엔지니어 목록")
    @GetMapping("/engineers")
    public List<EngineerResponse> getEngineers() {
        return requestService.getEngineers(CurrentUser.companyId());
    }
}
