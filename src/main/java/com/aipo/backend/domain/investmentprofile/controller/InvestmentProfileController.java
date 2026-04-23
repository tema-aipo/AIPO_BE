package com.aipo.backend.domain.investmentprofile.controller;

import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileQuestionsResponse;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileResultResponse;
import com.aipo.backend.domain.investmentprofile.dto.SkipInvestmentProfileRequest;
import com.aipo.backend.domain.investmentprofile.dto.SubmitInvestmentProfileResultRequest;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
import com.aipo.backend.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Investment Profile", description = "투자성향 검사 API")
public class InvestmentProfileController {

    private final InvestmentProfileService investmentProfileService;

    @GetMapping("/questions")
    @Operation(summary = "투자성향 검사 문항 조회", description = "회원가입 또는 재검사 화면에서 사용할 투자성향 질문과 선택지를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "문항 조회 성공", content = @Content(schema = @Schema(implementation = InvestmentProfileQuestionsResponse.class)))
    })
    public ResponseEntity<InvestmentProfileQuestionsResponse> getQuestions() {
        return ResponseEntity.ok(investmentProfileService.getQuestions());
    }

    @PostMapping("/results")
    @Operation(summary = "회원가입 단계 투자성향 검사 제출", description = "회원가입 흐름에서 선택한 답변을 제출하고 투자성향 결과를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "투자성향 결과 반환", content = @Content(schema = @Schema(implementation = InvestmentProfileResultResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패 또는 제출값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "문항을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
