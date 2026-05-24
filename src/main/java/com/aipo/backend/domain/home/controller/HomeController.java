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
import org.springframework.web.bind.annotation.*;

@RestController // JSON 응답을 반환하는 REST API 컨트롤러
@RequiredArgsConstructor // final 필드 생성자 자동 생성
@RequestMapping("/api/v1/home") // 홈 API 기본 경로
@Tag(name = "Home", description = "홈 화면 공모주 요약 API")
public class HomeController {

    private final HomeService homeService;

    @GetMapping // GET /api/home
    @Operation(summary = "홈 화면 조회", description = "홈 화면의 대표 공모주, 조회 급등 공모주, 매력지수 영역을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "홈 화면 조회 성공", content = @Content(schema = @Schema(implementation = HomeResponse.class)))
    })
    public ResponseEntity<HomeResponse> getHome(
            // tab이 없으면 기본값 recentGrowth 사용
            @Parameter(description = "매력지수 탭. recentGrowth, subscriptionUpcoming, favorite 지원", example = "recentGrowth")
            @RequestParam(required = false, defaultValue = "recentGrowth") String tab,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        // 서비스에 tab을 넘겨서 홈 데이터 생성
        Long userId = principal != null ? principal.getUserId() : null;
        HomeResponse response = homeService.getHome(tab, userId);

        // 200 OK + 응답 바디 반환
        return ResponseEntity.ok(response);
    }
}
