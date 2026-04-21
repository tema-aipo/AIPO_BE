package com.aipo.backend.domain.ipo.controller;

import com.aipo.backend.domain.ipo.dto.AttractionReasonItem;
import com.aipo.backend.domain.ipo.dto.AttractionSection;
import com.aipo.backend.domain.ipo.dto.CompetitionInfo;
import com.aipo.backend.domain.ipo.dto.CompetitionTab;
import com.aipo.backend.domain.ipo.dto.DateRange;
import com.aipo.backend.domain.ipo.dto.DemandForecastSection;
import com.aipo.backend.domain.ipo.dto.DepositInfoItem;
import com.aipo.backend.domain.ipo.dto.IpoDetailResponse;
import com.aipo.backend.domain.ipo.dto.OfferingInfoSection;
import com.aipo.backend.domain.ipo.dto.ScheduleSection;
import com.aipo.backend.domain.ipo.dto.SubscriptionCompetitionSection;
import com.aipo.backend.domain.ipo.dto.SummarySection;
import com.aipo.backend.domain.ipo.service.IpoService;
import com.aipo.backend.global.config.SecurityConfig;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import com.aipo.backend.global.security.service.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IpoController.class)
@Import(SecurityConfig.class)
class IpoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IpoService ipoService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("정상 조회 시 200과 상세 구조를 반환한다")
    void getIpoDetail_success() throws Exception {
        Long ipoId = 1L;
        IpoDetailResponse response = createResponse(ipoId);

        when(ipoService.getIpoDetail(ipoId, null)).thenReturn(response);

        mockMvc.perform(get("/api/v1/ipos/{ipoId}", ipoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ipoId").value(ipoId))
                .andExpect(jsonPath("$.summary.companyName").value("AIPO"))
                .andExpect(jsonPath("$.attraction.totalScore").value(87.5))
                .andExpect(jsonPath("$.demandForecast.participatingInstitutionCount").value(1234))
                .andExpect(jsonPath("$.subscriptionCompetition.defaultTab").value("EQUAL"))
                .andExpect(jsonPath("$.schedule.refundDate").value("2026-05-02"))
                .andExpect(jsonPath("$.depositInfos[0].securitiesCompanyName").value("증권사A"))
                .andExpect(jsonPath("$.offeringInfo.marketCap").value(250000000000.00));

        verify(ipoService).getIpoDetail(ipoId, null);
    }

    @Test
    @DisplayName("존재하지 않는 ipoId 조회 시 공통 예외 응답을 반환한다")
    void getIpoDetail_whenIpoDoesNotExist_returnsErrorResponse() throws Exception {
        Long ipoId = 999L;

        when(ipoService.getIpoDetail(ipoId, null))
                .thenThrow(new CustomException(ErrorCode.IPO_NOT_FOUND));

        mockMvc.perform(get("/api/v1/ipos/{ipoId}", ipoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("IPO_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("비로그인 사용자도 상세 조회 엔드포인트를 호출할 수 있다")
    void getIpoDetail_anonymousUserCanAccess() throws Exception {
        Long ipoId = 2L;

        when(ipoService.getIpoDetail(ipoId, null)).thenReturn(createResponse(ipoId));

        mockMvc.perform(get("/api/v1/ipos/{ipoId}", ipoId)
                        .with(SecurityMockMvcRequestPostProcessors.anonymous()))
                .andExpect(status().isOk());

        verify(ipoService).getIpoDetail(ipoId, null);
    }

    @Test
    @DisplayName("로그인 principal 이 있어도 동일 엔드포인트가 정상 동작한다")
    void getIpoDetail_authenticatedPrincipalCanAccess() throws Exception {
        Long ipoId = 3L;
        Long userId = 10L;
        CustomUserDetails principal = new CustomUserDetails(userId, "investor", "USER");

        when(ipoService.getIpoDetail(ipoId, userId)).thenReturn(createResponse(ipoId));

        mockMvc.perform(get("/api/v1/ipos/{ipoId}", ipoId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ipoId").value(ipoId));

        verify(ipoService).getIpoDetail(ipoId, userId);
    }

    @Test
    @DisplayName("응답 JSON에 상세 조회 섹션 구조가 모두 존재한다")
    void getIpoDetail_containsExpectedJsonStructure() throws Exception {
        Long ipoId = 4L;

        when(ipoService.getIpoDetail(ipoId, null)).thenReturn(createResponse(ipoId));

        mockMvc.perform(get("/api/v1/ipos/{ipoId}", ipoId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ipoId").exists())
                .andExpect(jsonPath("$.summary").isMap())
                .andExpect(jsonPath("$.attraction").isMap())
                .andExpect(jsonPath("$.demandForecast").isMap())
                .andExpect(jsonPath("$.subscriptionCompetition").isMap())
                .andExpect(jsonPath("$.schedule").isMap())
                .andExpect(jsonPath("$.depositInfos").isArray())
                .andExpect(jsonPath("$.offeringInfo").isMap());

        verify(ipoService).getIpoDetail(ipoId, null);
    }

    private IpoDetailResponse createResponse(Long ipoId) {
        return new IpoDetailResponse(
                ipoId,
                new SummarySection(
                        "AIPO",
                        "공시문서 분석 기업",
                        new BigDecimal("15000.00"),
                        List.of("주관사A", "주관사B"),
                        new DateRange(LocalDate.of(2026, 4, 28), LocalDate.of(2026, 4, 29)),
                        false
                ),
                new AttractionSection(
                        new BigDecimal("87.5"),
                        List.of(
                                new AttractionReasonItem(1, "수요예측 강세", "기관 반응이 우수합니다.")
                        )
                ),
                new DemandForecastSection(
                        new BigDecimal("1234.56"),
                        1234,
                        new BigDecimal("90.12"),
                        1111,
                        new BigDecimal("45.67"),
                        999,
                        new BigDecimal("12.34")
                ),
                new SubscriptionCompetitionSection(
                        CompetitionTab.EQUAL,
                        new CompetitionInfo(new BigDecimal("2.5"), new BigDecimal("150.25")),
                        new CompetitionInfo(new BigDecimal("1.2"), new BigDecimal("320.75"))
                ),
                new ScheduleSection(
                        new DateRange(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 21)),
                        new DateRange(LocalDate.of(2026, 4, 28), LocalDate.of(2026, 4, 29)),
                        LocalDate.of(2026, 5, 2),
                        LocalDate.of(2026, 5, 8)
                ),
                List.of(
                        new DepositInfoItem(1, "증권사A", new BigDecimal("75000.00"))
                ),
                new OfferingInfoSection(
                        new BigDecimal("250000000000.00"),
                        new BigDecimal("50.00"),
                        new BigDecimal("18.40"),
                        new BigDecimal("5.10")
                )
        );
    }
}
