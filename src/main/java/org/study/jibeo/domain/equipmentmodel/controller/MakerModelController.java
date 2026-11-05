package org.study.jibeo.domain.equipmentmodel.controller;

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
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.equipmentmodel.dto.CreateModelRequest;
import org.study.jibeo.domain.equipmentmodel.dto.CreatePartRequest;
import org.study.jibeo.domain.equipmentmodel.dto.MakerModelResponse;
import org.study.jibeo.domain.equipmentmodel.dto.ModelDetailResponse;
import org.study.jibeo.domain.equipmentmodel.dto.PartResponse;
import org.study.jibeo.domain.equipmentmodel.dto.ManualResponse;
import org.study.jibeo.domain.equipmentmodel.dto.UpdateManualRequest;
import org.study.jibeo.domain.equipmentmodel.service.EquipmentModelService;
import org.study.jibeo.global.security.CurrentUser;

import java.util.List;

/** 제작 업체(STAFF) 전용: 우리 회사 모델·부품·매뉴얼 관리 */
@Tag(name = "모델·부품·매뉴얼 (제작 업체)")
@RestController
@RequestMapping("/api/maker/models")
@RequiredArgsConstructor
public class MakerModelController {

    private final EquipmentModelService modelService;

    @Operation(summary = "우리 모델 목록")
    @GetMapping
    public List<MakerModelResponse> getMyModels() {
        return modelService.getMakerModels(CurrentUser.companyId());
    }

    @Operation(summary = "모델 등록")
    @PostMapping
    public ModelDetailResponse createModel(@Valid @RequestBody CreateModelRequest request) {
        return modelService.createModel(CurrentUser.companyId(), request);
    }

    @Operation(summary = "부품 등록")
    @PostMapping("/{modelId}/parts")
    public PartResponse createPart(@PathVariable Long modelId, @Valid @RequestBody CreatePartRequest request) {
        return modelService.createPart(CurrentUser.companyId(), modelId, request);
    }

    /** content = 입력한 전문, sections = 서버가 제목 기준으로 나눈 AI 검색 항목 */
    @Operation(summary = "매뉴얼 조회")
    @GetMapping("/{modelId}/manual")
    public ManualResponse getManual(@PathVariable Long modelId) {
        return modelService.getManual(CurrentUser.companyId(), modelId);
    }

    /** 매뉴얼 전문을 통째로 교체. 마크다운 제목(#, ## …)마다 AI 검색 항목으로 나뉘고 AI 가 다시 색인한다 */
    @Operation(summary = "매뉴얼 수정", description = "content 에 매뉴얼 전문(마크다운)을 넣는다. 제목마다 항목으로 나뉘며, "
            + "제목에 안전 수칙·주의사항·경고가 있으면 안전 항목(모든 해결책의 주의사항에 붙음), 나머지는 상담 검색 대상이 된다.")
    @PutMapping("/{modelId}/manual")
    public ManualResponse updateManual(@PathVariable Long modelId, @Valid @RequestBody UpdateManualRequest request) {
        return modelService.updateManual(CurrentUser.companyId(), modelId, request.content());
    }
}
