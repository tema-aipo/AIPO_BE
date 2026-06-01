package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.auth.dto.LoginRequest;
import com.aipo.backend.domain.auth.dto.LoginResponse;
import com.aipo.backend.domain.auth.entity.UserRefreshToken;
import com.aipo.backend.domain.auth.repository.UserRefreshTokenRepository;
import com.aipo.backend.domain.log.service.LoginLogService;
import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.entity.UserRole;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import com.aipo.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "관리자 인증", description = "관리자 로그인 API")
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

    // ✨ 새롭게 추가된 로그아웃 기능
    @Operation(summary = "관리자 로그아웃")
    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<?> logout(@AuthenticationPrincipal CustomUserDetails principal) {
        // 이미 토큰이 만료되었거나 없는 상태에서의 요청으로 인한 500 에러(NullPointerException) 방지
        if (principal != null) {
            // DB에서 해당 유저의 리프레시 토큰을 삭제하여 완전한 로그아웃 처리
            userRefreshTokenRepository.deleteByUser_UserId(principal.getUserId());
        }

        // 프론트엔드에게 성공 메시지 반환
        return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "성공적으로 로그아웃 되었습니다."
        ));
    }
}