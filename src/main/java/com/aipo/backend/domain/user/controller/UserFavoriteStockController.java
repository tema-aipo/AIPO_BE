package com.aipo.backend.domain.user.controller;

import com.aipo.backend.domain.user.dto.FavoriteStockResponse;
import com.aipo.backend.domain.user.dto.UpdateFavoriteNotificationRequest;
import com.aipo.backend.domain.user.service.FavoriteService;
import com.aipo.backend.global.config.OpenApiConfig;
import com.aipo.backend.global.exception.ErrorResponse;
import com.aipo.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/favorites")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
@Tag(name = "Favorite", description = "사용자 관심종목 API")
public class UserFavoriteStockController {

    private final FavoriteService favoriteService;

    @GetMapping
    @Operation(summary = "관심종목 목록 조회", description = "현재 로그인 사용자의 관심종목 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관심종목 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<FavoriteStockResponse> getFavorites(@AuthenticationPrincipal CustomUserDetails principal) {
        return favoriteService.getFavorites(principal.getUserId());
    }

    @PostMapping("/{ipoId}")
    @Operation(summary = "관심종목 등록", description = "현재 로그인 사용자의 관심종목에 공모주를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "관심종목 등록 성공", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "공모주를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 등록된 관심종목", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> addFavorite(
            @Parameter(description = "공모주 식별자", example = "1")
            @PathVariable Long ipoId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        favoriteService.addFavorite(principal.getUserId(), ipoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{ipoId}")
    @Operation(summary = "관심종목 삭제", description = "현재 로그인 사용자의 관심종목에서 공모주를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "관심종목 삭제 성공", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "관심종목을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> removeFavorite(
            @Parameter(description = "공모주 식별자", example = "1")
            @PathVariable Long ipoId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        favoriteService.removeFavorite(principal.getUserId(), ipoId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{ipoId}/notification")
    @Operation(summary = "관심종목 알림 설정", description = "관심종목별 알림 ON/OFF를 수정합니다.")
    public ResponseEntity<FavoriteStockResponse> updateFavoriteNotification(
            @PathVariable Long ipoId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody UpdateFavoriteNotificationRequest request
    ) {
        return ResponseEntity.ok(
                favoriteService.updateFavoriteNotification(principal.getUserId(), ipoId, request.enabled())
        );
    }
}
