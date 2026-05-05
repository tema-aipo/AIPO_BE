package com.aipo.backend.domain.auth.controller;

import com.aipo.backend.domain.auth.dto.*;
import com.aipo.backend.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입, 로그인, 토큰 재발급, 로그아웃")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입")
    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "액세스 토큰 재발급")
    @PostMapping("/reissue")
    public ReissueResponse reissue(@Valid @RequestBody ReissueRequest request) {
        return authService.reissue(request);
    }

    @Operation(summary = "로그아웃", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public MessageResponse logout(@RequestHeader("Authorization") String authorizationHeader) {
        return authService.logout(authorizationHeader);
    }
}
// NOTE:
// 현재는 인증 핵심 API만 제공한다.
// 추후 아이디 중복 체크, 이메일 인증, 비밀번호 재설정,
// 공통 응답 포맷 적용에 따라 엔드포인트와 응답 구조가 일부 확장될 수 있다.