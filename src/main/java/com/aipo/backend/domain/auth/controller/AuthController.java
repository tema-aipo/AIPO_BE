package com.aipo.backend.domain.auth.controller;

import com.aipo.backend.domain.auth.dto.*;
import com.aipo.backend.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/reissue")
    public ReissueResponse reissue(@Valid @RequestBody ReissueRequest request) {
        return authService.reissue(request);
    }

    @PostMapping("/logout")
    public MessageResponse logout(@RequestHeader("Authorization") String authorizationHeader) {
        return authService.logout(authorizationHeader);
    }
}
// NOTE:
// 현재는 인증 핵심 API만 제공한다.
// 추후 아이디 중복 체크, 이메일 인증, 비밀번호 재설정,
// 공통 응답 포맷 적용에 따라 엔드포인트와 응답 구조가 일부 확장될 수 있다.