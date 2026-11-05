package org.study.jibeo.domain.request.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.request.dto.CreateRequestRequest;
import org.study.jibeo.domain.request.dto.RequestDetailResponse;
import org.study.jibeo.domain.request.dto.RequestSummaryResponse;
import org.study.jibeo.domain.request.service.RequestService;
import org.study.jibeo.global.security.CurrentUser;

import java.util.List;

/** 사용 업체(WORKER) 전용: 담당자 호출 */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    /** 호출 신고서 제출 → 요청번호 발급, 상태 RECEIVED */
    @PostMapping
    public RequestDetailResponse create(@Valid @RequestBody CreateRequestRequest request) {
        return requestService.create(CurrentUser.id(), CurrentUser.companyId(), request);
    }

    /** 우리 회사가 보낸 호출 목록 (최신순) */
    @GetMapping
    public List<RequestSummaryResponse> getMyRequests() {
        return requestService.getMyRequests(CurrentUser.companyId());
    }

    /** 호출 진행 상태 (접수 → 배정 → 완료) */
    @GetMapping("/{requestId}")
    public RequestDetailResponse getMyRequest(@PathVariable Long requestId) {
        return requestService.getMyRequest(CurrentUser.companyId(), requestId);
    }
}
