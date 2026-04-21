package com.aipo.backend.domain.user.controller;

import com.aipo.backend.domain.user.dto.FavoriteStockResponse;
import com.aipo.backend.domain.user.service.FavoriteService;
import com.aipo.backend.global.config.OpenApiConfig;
import com.aipo.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/favorites")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
public class UserFavoriteStockController {

    private final FavoriteService favoriteService;

    @GetMapping
    @Operation(summary = "관심종목 목록 조회", description = "현재 로그인 사용자의 관심종목 목록을 조회합니다.")
    public List<FavoriteStockResponse> getFavorites(@AuthenticationPrincipal CustomUserDetails principal) {
        return favoriteService.getFavorites(principal.getUserId());
    }

    @PostMapping("/{ipoId}")
    @Operation(summary = "관심종목 등록", description = "현재 로그인 사용자의 관심종목에 공모주를 등록합니다.")
    public ResponseEntity<Void> addFavorite(
            @PathVariable Long ipoId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        favoriteService.addFavorite(principal.getUserId(), ipoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{ipoId}")
    @Operation(summary = "관심종목 삭제", description = "현재 로그인 사용자의 관심종목에서 공모주를 삭제합니다.")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long ipoId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        favoriteService.removeFavorite(principal.getUserId(), ipoId);
        return ResponseEntity.noContent().build();
    }
}
