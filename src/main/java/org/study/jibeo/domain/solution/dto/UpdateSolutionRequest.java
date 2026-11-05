package org.study.jibeo.domain.solution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 솔루션 전체 수정 (단계는 순서대로 전체 교체) */
public record UpdateSolutionRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 50) String category,
        @Size(max = 20) List<@NotBlank @Size(max = 100) String> symptoms,
        @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 500) String> steps,
        @Size(max = 2000) String fallback
) {
}
