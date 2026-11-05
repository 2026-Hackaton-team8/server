package org.study.jibeo.domain.equipmentmodel.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.equipmentmodel.dto.CreateModelRequest;
import org.study.jibeo.domain.equipmentmodel.dto.CreatePartRequest;
import org.study.jibeo.domain.equipmentmodel.dto.MakerModelResponse;
import org.study.jibeo.domain.equipmentmodel.dto.ModelDetailResponse;
import org.study.jibeo.domain.equipmentmodel.dto.PartResponse;
import org.study.jibeo.domain.equipmentmodel.model.Manual;
import org.study.jibeo.domain.equipmentmodel.service.EquipmentModelService;
import org.study.jibeo.global.security.CurrentUser;

import java.util.List;

/** 제작 업체(STAFF) 전용: 우리 회사 모델·부품·매뉴얼 관리 */
@RestController
@RequestMapping("/api/maker/models")
@RequiredArgsConstructor
public class MakerModelController {

    private final EquipmentModelService modelService;

    @GetMapping
    public List<MakerModelResponse> getMyModels() {
        return modelService.getMakerModels(CurrentUser.companyId());
    }

    @PostMapping
    public ModelDetailResponse createModel(@Valid @RequestBody CreateModelRequest request) {
        return modelService.createModel(CurrentUser.companyId(), request);
    }

    @PostMapping("/{modelId}/parts")
    public PartResponse createPart(@PathVariable Long modelId, @Valid @RequestBody CreatePartRequest request) {
        return modelService.createPart(CurrentUser.companyId(), modelId, request);
    }

    @GetMapping("/{modelId}/manual")
    public Manual getManual(@PathVariable Long modelId) {
        return modelService.getManual(CurrentUser.companyId(), modelId);
    }

    /** 매뉴얼 전체 교체 */
    @PutMapping("/{modelId}/manual")
    public Manual updateManual(@PathVariable Long modelId, @Valid @RequestBody Manual manual) {
        return modelService.updateManual(CurrentUser.companyId(), modelId, manual);
    }
}
