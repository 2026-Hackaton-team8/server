package org.study.jibeo.domain.equipment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.equipment.dto.CreateEquipmentRequest;
import org.study.jibeo.domain.equipment.dto.EquipmentDetailResponse;
import org.study.jibeo.domain.equipment.dto.EquipmentResponse;
import org.study.jibeo.domain.equipment.service.EquipmentService;
import org.study.jibeo.domain.repairhistory.dto.RepairHistoryResponse;
import org.study.jibeo.domain.repairhistory.service.RepairHistoryService;
import org.study.jibeo.global.security.CurrentUser;

import java.util.List;

/** 사용 업체(WORKER) 전용: 우리 회사 설치 설비 */
@Tag(name = "설비 (사용 업체)")
@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final RepairHistoryService repairHistoryService;

    @Operation(summary = "내 설비 목록")
    @GetMapping
    public List<EquipmentResponse> getMyEquipments() {
        return equipmentService.getMyEquipments(CurrentUser.companyId());
    }

    @Operation(summary = "설비 등록")
    @PostMapping
    public EquipmentDetailResponse create(@Valid @RequestBody CreateEquipmentRequest request) {
        return equipmentService.create(CurrentUser.companyId(), request);
    }

    @Operation(summary = "설비 상세")
    @GetMapping("/{equipmentId}")
    public EquipmentDetailResponse getDetail(@PathVariable Long equipmentId) {
        return equipmentService.getDetail(CurrentUser.companyId(), equipmentId);
    }

    /** 이 설비의 수리 이력 (최신순) */
    @Operation(summary = "설비 수리 이력")
    @GetMapping("/{equipmentId}/repair-histories")
    public List<RepairHistoryResponse> getRepairHistories(@PathVariable Long equipmentId) {
        return repairHistoryService.getEquipmentHistories(CurrentUser.companyId(), equipmentId);
    }
}
