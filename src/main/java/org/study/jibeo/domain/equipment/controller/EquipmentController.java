package org.study.jibeo.domain.equipment.controller;

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
import org.study.jibeo.global.security.CurrentUser;

import java.util.List;

/** 사용 업체(WORKER) 전용: 우리 회사 설치 설비 */
@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    public List<EquipmentResponse> getMyEquipments() {
        return equipmentService.getMyEquipments(CurrentUser.companyId());
    }

    @PostMapping
    public EquipmentDetailResponse create(@Valid @RequestBody CreateEquipmentRequest request) {
        return equipmentService.create(CurrentUser.companyId(), request);
    }

    @GetMapping("/{equipmentId}")
    public EquipmentDetailResponse getDetail(@PathVariable Long equipmentId) {
        return equipmentService.getDetail(CurrentUser.companyId(), equipmentId);
    }
}
