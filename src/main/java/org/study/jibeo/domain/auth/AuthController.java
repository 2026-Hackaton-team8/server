package org.study.jibeo.domain.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.auth.dto.LoginIdCheckResponse;
import org.study.jibeo.domain.auth.dto.LoginRequest;
import org.study.jibeo.domain.auth.dto.LoginResponse;
import org.study.jibeo.domain.auth.dto.LogoutRequest;
import org.study.jibeo.domain.auth.dto.RefreshRequest;
import org.study.jibeo.domain.auth.dto.RefreshResponse;
import org.study.jibeo.domain.auth.dto.SignUpRequest;
import org.study.jibeo.domain.auth.dto.SignUpResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    @GetMapping("/login-id/available")
    public LoginIdCheckResponse checkLoginId(@RequestParam @NotBlank String loginId) {
        return new LoginIdCheckResponse(authService.isLoginIdAvailable(loginId));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
