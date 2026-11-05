package org.study.jibeo.domain.request.dto;

import org.study.jibeo.domain.user.model.User;

public record EngineerResponse(Long id, String name, String phone, String email) {

    public static EngineerResponse from(User user) {
        return new EngineerResponse(user.getId(), user.getName(), user.getPhone(), user.getEmail());
    }
}
