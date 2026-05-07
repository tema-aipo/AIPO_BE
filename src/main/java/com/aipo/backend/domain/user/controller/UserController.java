package com.aipo.backend.domain.user.controller;

import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.entity.UserRole;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
import com.aipo.backend.global.config.OpenApiConfig;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import com.aipo.backend.global.exception.ErrorResponse;
import com.aipo.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 기본 정보 API")
public class UserController {

    private final UserRepository userRepository;
    private final InvestmentProfileService investmentProfileService;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 accessToken에 해당하는 사용자의 기본 정보를 조회합니다.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 정보 조회 성공", content = @Content(schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public MeResponse me(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = userRepository.findByLoginId(principal.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String investmentType = "분석대기중";
        try {
            investmentType = investmentProfileService.getCurrentResult(user.getUserId()).profileLabel();
        } catch (Exception ignored) {}

        return new MeResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getUserName(),
                user.getEmail(),
                user.getRole(),
                investmentType
        );
    }

    @Getter
    @AllArgsConstructor
    public static class MeResponse {
        private Long userId;
        private String loginId;
        private String userName;
        private String email;
        private UserRole role;
        private String investmentType;
    }
}
