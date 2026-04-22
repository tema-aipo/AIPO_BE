package com.aipo.backend.domain.investmentprofile.controller;

import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileAnswerRequest;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileOptionItem;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileQuestionItem;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileQuestionsResponse;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileResultResponse;
import com.aipo.backend.domain.investmentprofile.dto.SkipInvestmentProfileRequest;
import com.aipo.backend.domain.investmentprofile.dto.SubmitInvestmentProfileResultRequest;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileTestStatus;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
import com.aipo.backend.global.config.SecurityConfig;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvestmentProfileController.class)
@Import(SecurityConfig.class)
class InvestmentProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InvestmentProfileService investmentProfileService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("투자 성향 문항 조회 API는 공개로 동작한다")
    void getQuestions_success() throws Exception {
        when(investmentProfileService.getQuestions()).thenReturn(new InvestmentProfileQuestionsResponse(
                1,
                List.of(new InvestmentProfileQuestionItem(
                        1L,
                        1,
                        "IPO 공모주 청약 참여 경험",
                        List.of(new InvestmentProfileOptionItem(101L, 1, "전혀 없음", 0))
                ))
        ));

        mockMvc.perform(get("/api/v1/investment-profile/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.questions[0].questionText").value("IPO 공모주 청약 참여 경험"));
    }

    @Test
    @DisplayName("검사 제출 API는 완료 화면 응답을 반환한다")
    void submitResult_success() throws Exception {
        when(investmentProfileService.submitResult(org.mockito.ArgumentMatchers.any())).thenReturn(new InvestmentProfileResultResponse(
                1L,
                1,
                InvestmentProfileTestStatus.COMPLETED,
                InvestmentProfileType.NEUTRAL,
                "중립형",
                "중립형 투자 성향으로 분석되었어요",
                "위험과 수익 사이의 균형을 고려하는 성향이에요.",
                List.of("위험 중립", "균형 추구"),
                "AIPO 시작하기",
                "LOGIN",
                9,
                null
        ));

        mockMvc.perform(post("/api/v1/investment-profile/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitInvestmentProfileResultRequest(
                                1L,
                                1,
                                List.of(
                                        new InvestmentProfileAnswerRequest(1L, 101L),
                                        new InvestmentProfileAnswerRequest(2L, 202L),
                                        new InvestmentProfileAnswerRequest(3L, 302L),
                                        new InvestmentProfileAnswerRequest(4L, 402L),
                                        new InvestmentProfileAnswerRequest(5L, 502L),
                                        new InvestmentProfileAnswerRequest(6L, 602L)
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.profileType").value("NEUTRAL"))
                .andExpect(jsonPath("$.profileLabel").value("중립형"));
    }

    @Test
    @DisplayName("검사 스킵 API는 분석 대기중 응답을 반환한다")
    void skip_success() throws Exception {
        when(investmentProfileService.skip(org.mockito.ArgumentMatchers.any())).thenReturn(new InvestmentProfileResultResponse(
                2L,
                1,
                InvestmentProfileTestStatus.SKIPPED,
                null,
                "분석 대기중",
                "분석 대기중",
                "기본 설정으로 먼저 시작할 수 있어요.",
                List.of("기본 설정"),
                "AIPO 시작하기",
                "LOGIN",
                null,
                null
        ));

        mockMvc.perform(post("/api/v1/investment-profile/skip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SkipInvestmentProfileRequest(1L, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testStatus").value("SKIPPED"))
                .andExpect(jsonPath("$.profileLabel").value("분석 대기중"));
    }
}
