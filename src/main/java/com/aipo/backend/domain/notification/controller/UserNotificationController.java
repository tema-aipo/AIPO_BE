package com.aipo.backend.domain.notification.controller;

import com.aipo.backend.domain.notification.dto.NotificationListResponse;
import com.aipo.backend.domain.notification.dto.UnreadNotificationCountResponse;
import com.aipo.backend.domain.notification.service.UserNotificationService;
import com.aipo.backend.global.config.OpenApiConfig;
import com.aipo.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/notifications")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
@Tag(name = "Notification", description = "사용자 알림 API")
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    @GetMapping("/items")
    @Operation(summary = "내 알림 목록 조회")
    public ResponseEntity<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(userNotificationService.getNotifications(principal.getUserId(), page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 개수 조회")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(userNotificationService.getUnreadCount(principal.getUserId()));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long notificationId
    ) {
        userNotificationService.markRead(principal.getUserId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        userNotificationService.markAllRead(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "알림 삭제")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long notificationId
    ) {
        userNotificationService.delete(principal.getUserId(), notificationId);
        return ResponseEntity.noContent().build();
    }
}
