package org.study.jibeo.domain.equipmentmodel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.equipmentmodel.dto.ModelDetailResponse;
import org.study.jibeo.domain.equipmentmodel.dto.ModelSummaryResponse;
import org.study.jibeo.domain.equipmentmodel.dto.PartResponse;
import org.study.jibeo.domain.equipmentmodel.service.EquipmentModelService;

import java.util.List;

@Tag(name = "모델 조회")
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final EquipmentModelService modelService;

    /** 모델 검색 (코드·이름) */
    @Operation(summary = "모델 검색")
    @GetMapping
    public List<ModelSummaryResponse> search(@RequestParam(required = false) String q) {
        return modelService.search(q);
    }

    @Operation(summary = "모델 상세")
    @GetMapping("/{modelId}")
    public ModelDetailResponse getDetail(@PathVariable Long modelId) {
        return modelService.getDetail(modelId);
    }

    @Operation(summary = "모델 부품 목록")
    @GetMapping("/{modelId}/parts")
    public List<PartResponse> getParts(@PathVariable Long modelId) {
        return modelService.getParts(modelId);
    }
}
