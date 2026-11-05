package org.study.jibeo.domain.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StartConsultationRequest(
        @NotNull Long equipmentId,
        @NotBlank @Size(max = 255) String symptom
) {
}
