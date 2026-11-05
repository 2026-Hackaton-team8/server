package org.study.jibeo.domain.equipmentmodel.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * equipment_model.manual 에 jsonb 로 저장되는 매뉴얼.
 * 섹션마다 증상·부품 코드가 붙어 있어 AI 가 상담 증상에 맞는 섹션을 바로 찾을 수 있다.
 */
public record Manual(
        int version,
        @NotNull @Size(max = 200) List<@Valid @NotNull Section> sections
) {

    public enum SectionType {
        OVERVIEW,
        OPERATION,
        MAINTENANCE,
        TROUBLESHOOTING,
        SAFETY
    }

    public record Section(
            @NotBlank @Size(max = 64) @Pattern(regexp = "^[a-z0-9-]+$", message = "영문 소문자, 숫자, - 만 사용할 수 있습니다") String id,
            @NotBlank @Size(max = 200) String title,
            @NotNull SectionType type,
            List<@NotBlank String> symptoms,
            List<@NotBlank String> partCodes,
            @NotBlank String content,
            List<@NotBlank String> steps,
            List<@NotBlank String> cautions
    ) {
    }
}
