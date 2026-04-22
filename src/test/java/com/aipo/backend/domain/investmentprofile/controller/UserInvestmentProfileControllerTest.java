package com.aipo.backend.domain.investmentprofile.controller;

import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileAnswerRequest;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileResultResponse;
import com.aipo.backend.domain.investmentprofile.dto.RetestInvestmentProfileRequest;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileTestStatus;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
import com.aipo.backend.global.config.SecurityConfig;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import com.aipo.backend.global.security.service.CustomUserDetails;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserInvestmentProfileController.class)
@Import(SecurityConfig.class)
class UserInvestmentProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InvestmentProfileService investmentProfileService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("내 투자 성향 결과를 조회한다")
    void getCurrentResult_success() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(1L, "demo-user", "USER");
        when(investmentProfileService.getCurrentResult(1L)).thenReturn(new InvestmentProfileResultResponse(
                3L,
                1,
                InvestmentProfileTestStatus.NOT_TESTED,
                null,
                null,
                "투자 성향 검사가 필요해요",
                "아직 투자 성향 검사를 진행하지 않았어요.",
                List.of(),
                "AIPO 시작하기",
                "LOGIN",
                null,
                null
        ));

        mockMvc.perform(get("/api/v1/users/me/investment-profile")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testStatus").value("NOT_TESTED"))
                .andExpect(jsonPath("$.title").value("투자 성향 검사가 필요해요"));
    }

    @Test
    @DisplayName("재검사 제출 시 완료 결과를 반환한다")
    void retest_success() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(1L, "demo-user", "USER");
        when(investmentProfileService.retest(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new InvestmentProfileResultResponse(
                        4L,
                        1,
                        InvestmentProfileTestStatus.COMPLETED,
                        InvestmentProfileType.STABLE,
                        "안정형",
                        "안정형 투자 성향으로 분석되었어요",
                        "변동성보다 안정적인 흐름을 선호하는 성향이에요.",
                        List.of("안정 추구"),
                        "AIPO 시작하기",
                        "LOGIN",
                        4,
                        null
                ));

        mockMvc.perform(post("/api/v1/users/me/investment-profile/retest")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RetestInvestmentProfileRequest(
                                1,
                                List.of(
                                        new InvestmentProfileAnswerRequest(1L, 101L),
                                        new InvestmentProfileAnswerRequest(2L, 201L),
                                        new InvestmentProfileAnswerRequest(3L, 301L),
                                        new InvestmentProfileAnswerRequest(4L, 401L),
                                        new InvestmentProfileAnswerRequest(5L, 501L),
                                        new InvestmentProfileAnswerRequest(6L, 601L)
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.profileType").value("STABLE"))
                .andExpect(jsonPath("$.profileLabel").value("안정형"));
    }
}
