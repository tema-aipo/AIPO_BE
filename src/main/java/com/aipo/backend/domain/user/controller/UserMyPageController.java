package com.aipo.backend.domain.user.controller;

import com.aipo.backend.domain.auth.dto.MessageResponse;
import com.aipo.backend.domain.user.dto.ChangePasswordRequest;
import com.aipo.backend.domain.user.dto.MyPageResponse;
import com.aipo.backend.domain.user.dto.NotificationSettingResponse;
import com.aipo.backend.domain.user.dto.UpdateNotificationSettingRequest;
import com.aipo.backend.domain.user.dto.UpdateUserProfileRequest;
import com.aipo.backend.domain.user.dto.UserProfileResponse;
import com.aipo.backend.domain.user.dto.WithdrawRequest;
import com.aipo.backend.domain.user.service.MyPageService;
import com.aipo.backend.domain.user.service.NotificationSettingService;
import com.aipo.backend.domain.user.service.UserProfileService;
import com.aipo.backend.global.config.OpenApiConfig;
import com.aipo.backend.global.exception.ErrorResponse;
import com.aipo.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
@Tag(name = "My Page", description = "마이페이지 및 사용자 설정 API")
public class UserMyPageController {

    private final MyPageService myPageService;
    private final NotificationSettingService notificationSettingService;
    private final UserProfileService userProfileService;

    @GetMapping("/mypage")
    @Operation(summary = "마이페이지 기본 조회", description = "사용자 이름, 투자성향 요약, 전역 알림 설정, 고객지원/앱 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이페이지 조회 성공", content = @Content(schema = @Schema(implementation = MyPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyPageResponse> getMyPage(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(myPageService.getMyPage(principal.getUserId()));
    }

    @GetMapping("/notifications")
    @Operation(summary = "전역 알림 설정 조회")
    public ResponseEntity<NotificationSettingResponse> getNotifications(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(notificationSettingService.getSettings(principal.getUserId()));
    }

    @PatchMapping("/notifications")
    @Operation(summary = "전역 알림 설정 수정")
    public ResponseEntity<NotificationSettingResponse> updateNotifications(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody UpdateNotificationSettingRequest request
    ) {
        return ResponseEntity.ok(notificationSettingService.updateSettings(principal.getUserId(), request));
    }

    @GetMapping("/profile")
    @Operation(summary = "내 정보 조회")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(userProfileService.getProfile(principal.getUserId()));
    }

    @PatchMapping("/profile")
    @Operation(summary = "내 정보 수정")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ResponseEntity.ok(userProfileService.updateProfile(principal.getUserId(), request));
    }

    @PatchMapping("/password")
    @Operation(summary = "비밀번호 변경")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return ResponseEntity.ok(userProfileService.changePassword(principal.getUserId(), request));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "회원탈퇴")
    public ResponseEntity<MessageResponse> withdraw(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody WithdrawRequest request
    ) {
        return ResponseEntity.ok(userProfileService.withdraw(principal.getUserId(), request.password()));
    }
}
