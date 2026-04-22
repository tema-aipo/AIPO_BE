package com.aipo.backend.domain.investmentprofile.controller;

import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileResultResponse;
import com.aipo.backend.domain.investmentprofile.dto.RetestInvestmentProfileRequest;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
import com.aipo.backend.global.config.OpenApiConfig;
import com.aipo.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
public class UserInvestmentProfileController {

    private final InvestmentProfileService investmentProfileService;

    @GetMapping
    @Operation(summary = "내 투자 성향 결과 조회")
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
