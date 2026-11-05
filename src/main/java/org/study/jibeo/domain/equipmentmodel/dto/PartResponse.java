package org.study.jibeo.domain.equipmentmodel.dto;

import org.study.jibeo.domain.part.model.Part;

public record PartResponse(Long id, String code, String name, String locationDesc, String meshName) {

    public static PartResponse from(Part part) {
        return new PartResponse(part.getId(), part.getCode(), part.getName(), part.getLocationDesc(), part.getMeshName());
    }
}
