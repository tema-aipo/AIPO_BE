package com.aipo.backend.domain.investmentprofile.controller;

import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileQuestionsResponse;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileResultResponse;
import com.aipo.backend.domain.investmentprofile.dto.SkipInvestmentProfileRequest;
import com.aipo.backend.domain.investmentprofile.dto.SubmitInvestmentProfileResultRequest;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/investment-profile")
public class InvestmentProfileController {

    private final InvestmentProfileService investmentProfileService;

    @GetMapping("/questions")
    @Operation(summary = "투자 성향 검사 문항 조회")
    public ResponseEntity<InvestmentProfileQuestionsResponse> getQuestions() {
        return ResponseEntity.ok(investmentProfileService.getQuestions());
    }

    @PostMapping("/results")
    @Operation(summary = "회원가입 단계 투자 성향 검사 제출")
    public ResponseEntity<InvestmentProfileResultResponse> submitResult(
            @Valid @RequestBody SubmitInvestmentProfileResultRequest request
    ) {
        return ResponseEntity.ok(investmentProfileService.submitResult(request));
    }

    @PostMapping("/skip")
    @Operation(summary = "회원가입 단계 투자 성향 검사 스킵")
    public ResponseEntity<InvestmentProfileResultResponse> skip(
            @Valid @RequestBody SkipInvestmentProfileRequest request
    ) {
        return ResponseEntity.ok(investmentProfileService.skip(request));
    }
}
