package org.study.jibeo.domain.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEquipmentRequest(
        @NotNull Long modelId,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String location,
        @Size(max = 50) String serialNumber
) {
}
