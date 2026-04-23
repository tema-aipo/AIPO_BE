package com.aipo.backend.domain.investmentprofile.controller;

import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileResultResponse;
import com.aipo.backend.domain.investmentprofile.dto.RetestInvestmentProfileRequest;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/investment-profile")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
@Tag(name = "User Investment Profile", description = "로그인 사용자 투자성향 API")
public class UserInvestmentProfileController {

    private final InvestmentProfileService investmentProfileService;

    @GetMapping
    @Operation(summary = "내 투자성향 결과 조회", description = "현재 로그인 사용자의 최신 투자성향 결과를 조회합니다. 미검사 사용자는 NOT_TESTED 상태를 받을 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "투자성향 결과 조회 성공", content = @Content(schema = @Schema(implementation = InvestmentProfileResultResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<InvestmentProfileResultResponse> getCurrentResult(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(investmentProfileService.getCurrentResult(principal.getUserId()));
    }

    @PostMapping("/retest")
    @Operation(summary = "투자 성향 재검사 제출")
    public ResponseEntity<InvestmentProfileResultResponse> retest(
            @Valid @RequestBody RetestInvestmentProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(investmentProfileService.retest(principal.getUserId(), request));
    }
}
