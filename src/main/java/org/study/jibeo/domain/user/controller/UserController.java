package org.study.jibeo.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.auth.AuthService;
import org.study.jibeo.domain.auth.dto.LoginResponse;
import org.study.jibeo.domain.user.dto.MeResponse;
import org.study.jibeo.domain.user.dto.SelectRoleRequest;
import org.study.jibeo.domain.user.dto.UpdateMeRequest;
import org.study.jibeo.domain.user.service.UserService;
import org.study.jibeo.global.security.CurrentUser;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping
    public MeResponse getMe() {
        return userService.getMe(CurrentUser.id());
    }

    @PatchMapping
    public MeResponse updateMe(@Valid @RequestBody UpdateMeRequest request) {
        return userService.updateMe(CurrentUser.id(), request);
    }

    /** 역할 미정(PENDING)일 때 역할과 소속 회사를 정한다 (한 번만). 새 토큰을 돌려준다 */
    @PostMapping("/role")
    public LoginResponse selectRole(@Valid @RequestBody SelectRoleRequest request) {
        return authService.selectRole(CurrentUser.id(), request);
    }
}
