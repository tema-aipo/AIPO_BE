package com.aipo.backend.domain.home.controller;

import com.aipo.backend.domain.home.dto.HomeResponse;
import com.aipo.backend.domain.home.service.HomeService;
import com.aipo.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/home")
@Tag(name = "Home", description = "홈 화면 공모주 요약 API")
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    @Operation(summary = "홈 화면 조회", description = "홈 화면에 필요한 공모주, 조회 급등 공모주, 매력지수 영역을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "홈 화면 조회 성공", content = @Content(schema = @Schema(implementation = HomeResponse.class)))
    })
    public ResponseEntity<HomeResponse> getHome(
            @Parameter(description = "매력지수 탭: recentGrowth, subscriptionUpcoming, favorite", example = "recentGrowth")
            @RequestParam(required = false, defaultValue = "recentGrowth") String tab,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        HomeResponse response = homeService.getHome(tab, userId);
        return ResponseEntity.ok(response);
    }
}
