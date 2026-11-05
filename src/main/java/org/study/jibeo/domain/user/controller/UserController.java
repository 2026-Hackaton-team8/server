package org.study.jibeo.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.user.dto.MeResponse;
import org.study.jibeo.domain.user.dto.UpdateMeRequest;
import org.study.jibeo.domain.user.service.UserService;
import org.study.jibeo.global.security.CurrentUser;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public MeResponse getMe() {
        return userService.getMe(CurrentUser.id());
    }

    @PatchMapping
    public MeResponse updateMe(@Valid @RequestBody UpdateMeRequest request) {
        return userService.updateMe(CurrentUser.id(), request);
    }
}
