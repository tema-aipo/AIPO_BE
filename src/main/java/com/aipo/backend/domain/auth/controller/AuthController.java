package com.aipo.backend.domain.auth.controller;

import com.aipo.backend.domain.auth.dto.LoginIdAvailabilityResponse;
import com.aipo.backend.domain.auth.dto.LoginRequest;
import com.aipo.backend.domain.auth.dto.LoginResponse;
import com.aipo.backend.domain.auth.dto.MessageResponse;
import com.aipo.backend.domain.auth.dto.RegisterRequest;
import com.aipo.backend.domain.auth.dto.RegisterResponse;
import com.aipo.backend.domain.auth.dto.ReissueRequest;
import com.aipo.backend.domain.auth.dto.ReissueResponse;
import com.aipo.backend.domain.auth.service.AuthService;
import com.aipo.backend.global.config.OpenApiConfig;
import com.aipo.backend.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auth", description = "회원가입, 로그인, 토큰 재발급 API")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login-id/availability")
    @Operation(summary = "로그인 아이디 중복 확인", description = "회원가입 전에 loginId 사용 가능 여부를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "아이디 사용 가능 여부 조회 성공", content = @Content(schema = @Schema(implementation = LoginIdAvailabilityResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoginIdAvailabilityResponse checkLoginIdAvailability(
            @RequestParam @NotBlank(message = "loginId는 비어 있을 수 없습니다.") String loginId
    ) {
        return authService.checkLoginIdAvailability(loginId);
    }

    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "신규 사용자를 등록하고 기본 투자성향/알림 설정을 초기화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공", content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "중복 아이디 또는 이메일", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "loginId/password로 로그인하고 accessToken과 refreshToken을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "비밀번호 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "탈퇴 사용자", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "refreshToken으로 새 accessToken을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공", content = @Content(schema = @Schema(implementation = ReissueResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 refreshToken", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "저장된 refreshToken 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ReissueResponse reissue(@Valid @RequestBody ReissueRequest request) {
        return authService.reissue(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Authorization 헤더의 accessToken을 검증하고 저장된 refreshToken을 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authorization 헤더 누락 또는 유효하지 않은 accessToken", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
    public MessageResponse logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return authService.logout(authorizationHeader);
    }
}
