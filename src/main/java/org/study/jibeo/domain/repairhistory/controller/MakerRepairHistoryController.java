package org.study.jibeo.domain.repairhistory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.repairhistory.dto.CreateRepairHistoryRequest;
import org.study.jibeo.domain.repairhistory.dto.RepairHistoryResponse;
import org.study.jibeo.domain.repairhistory.service.RepairHistoryService;
import org.study.jibeo.global.security.CurrentUser;

import java.util.List;

/** 제작 업체(STAFF) 전용: 수리 이력 기록·조회 */
@RestController
@RequestMapping("/api/maker")
@RequiredArgsConstructor
public class MakerRepairHistoryController {

    private final RepairHistoryService repairHistoryService;

    /** 수리 이력 기록 → 호출 COMPLETED */
    @PostMapping("/requests/{requestId}/repair-history")
    public RepairHistoryResponse record(@PathVariable Long requestId,
                                        @Valid @RequestBody CreateRepairHistoryRequest request) {
        return repairHistoryService.record(CurrentUser.companyId(), requestId, request);
    }

    /** 모델 수리 이력 (다른 이용 기업 포함, 최신순) */
    @GetMapping("/models/{modelId}/repair-histories")
    public List<RepairHistoryResponse> getModelHistories(@PathVariable Long modelId) {
        return repairHistoryService.getModelHistories(CurrentUser.companyId(), modelId);
    }
}
