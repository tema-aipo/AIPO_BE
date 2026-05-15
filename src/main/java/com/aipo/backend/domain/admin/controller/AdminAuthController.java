package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.auth.dto.LoginRequest;
import com.aipo.backend.domain.auth.dto.LoginResponse;
import com.aipo.backend.domain.log.service.LoginLogService;
import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.entity.UserRole;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.domain.auth.entity.UserRefreshToken;
import com.aipo.backend.domain.auth.repository.UserRefreshTokenRepository;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 인증", description = "관리자 로그인 및 로그아웃")
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginLogService loginLogService;

    @Operation(summary = "관리자 로그인")
    @PostMapping("/login")
    @Transactional
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getLoginId(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BadCredentialsException("사용자를 찾을 수 없습니다."));

        if (user.getRole() != UserRole.ADMIN) {
            throw new BadCredentialsException("관리자 계정이 아닙니다.");
        }

        user.updateLastLoginAt();
        loginLogService.record(user);
        userRepository.save(user);

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getLoginId(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getLoginId());

        userRefreshTokenRepository.deleteByUser_UserId(user.getUserId());
        userRefreshTokenRepository.save(
                new UserRefreshToken(
                        user,
                        refreshToken,
                        jwtTokenProvider.getRefreshTokenExpiryDate()
                )
        );

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                user.getUserId(),
                user.getLoginId(),
                user.getUserName(),
                user.getEmail(),
                null
        );
    }

    // ✨ 새롭게 추가된 로그아웃 API 입니다!
    @Operation(summary = "관리자 로그아웃", description = "DB에 저장된 리프레시 토큰을 삭제합니다.")
    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<?> logout(Authentication authentication) {
        // 1. 헤더에 토큰이 없거나 인증되지 않은 사용자면 에러 반환
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body("인증 정보가 없거나 유효하지 않은 토큰입니다.");
        }

        // 2. 토큰에서 로그인 ID 추출
        String loginId = authentication.getName();

        // 3. 로그인 ID로 유저를 찾아서 DB에 있는 리프레시 토큰을 깔끔하게 지움
        userRepository.findByLoginId(loginId).ifPresent(user -> {
            userRefreshTokenRepository.deleteByUser_UserId(user.getUserId());
        });

        return ResponseEntity.ok("로그아웃이 성공적으로 완료되었습니다.");
    }
}
