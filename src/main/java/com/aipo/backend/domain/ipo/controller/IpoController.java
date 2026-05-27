package com.aipo.backend.domain.ipo.controller;

import com.aipo.backend.domain.ipo.dto.IpoDetailResponse;
import com.aipo.backend.domain.ipo.dto.IpoListResponse;
import com.aipo.backend.domain.ipo.service.IpoService;
import com.aipo.backend.global.exception.ErrorResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ipos")
@Tag(name = "IPO", description = "공모주 목록 및 상세 조회 API")
public class IpoController {

    private final IpoService ipoService;

    @GetMapping
    @Operation(summary = "공모주 목록 조회", description = "Flutter 탐색 화면에서 사용하는 공모주 목록을 검색, 정렬, 페이지네이션 기준으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공모주 목록 조회 성공", content = @Content(schema = @Schema(implementation = IpoListResponse.class)))
    })
    public ResponseEntity<IpoListResponse> getIpos(
            @Parameter(description = "페이지 번호. 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기. 최대 50으로 제한", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "법인명(corp_name) 또는 종목코드(stock_code) 검색어", example = "005930")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "정렬 필드. subscriptionStartDate, subscriptionEndDate, listingDate, confirmedOfferPrice, attractionScore, recentGrowthScore, stockName, companyName 지원", example = "subscriptionStartDate")
            @RequestParam(defaultValue = "subscriptionStartDate") String sort,
            @Parameter(description = "정렬 방향. asc 또는 desc", example = "asc")
            @RequestParam(defaultValue = "asc") String direction,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        IpoListResponse response = ipoService.getIpos(page, size, keyword, sort, direction, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ipoId}")
    @Operation(summary = "공모주 상세 조회", description = "공모주 상세 화면에 필요한 요약, 매력지수, 수요예측, 경쟁률, 일정, 증거금, 공모 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공모주 상세 조회 성공", content = @Content(schema = @Schema(implementation = IpoDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "공모주를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<IpoDetailResponse> getIpoDetail(
            @Parameter(description = "공모주 식별자", example = "1")
            @PathVariable Long ipoId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        IpoDetailResponse response = ipoService.getIpoDetail(ipoId, userId);
        return ResponseEntity.ok(response);
    }
}
