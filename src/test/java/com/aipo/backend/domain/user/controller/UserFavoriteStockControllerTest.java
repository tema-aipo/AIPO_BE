package com.aipo.backend.domain.user.controller;

import com.aipo.backend.domain.ipo.dto.DateRange;
import com.aipo.backend.domain.user.dto.FavoriteStockResponse;
import com.aipo.backend.domain.user.service.FavoriteService;
import com.aipo.backend.global.config.SecurityConfig;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import com.aipo.backend.global.security.service.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserFavoriteStockController.class)
@Import(SecurityConfig.class)
class UserFavoriteStockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("로그인 사용자는 내 관심종목 목록을 조회할 수 있다")
    void getFavorites_success() throws Exception {
        Long userId = 10L;
        CustomUserDetails principal = new CustomUserDetails(userId, "investor", "USER");

        when(favoriteService.getFavorites(userId)).thenReturn(List.of(
                new FavoriteStockResponse(
                        1L,
                        "AIPO",
                        "공시문서 분석 기업",
                        new BigDecimal("15000.00"),
                        new DateRange(LocalDate.of(2026, 4, 28), LocalDate.of(2026, 4, 29)),
                        true,
                        85,
                        "대표주관사",
                        "청약",
                        "04.28 ~ 04.29",
                        true
                )
        ));

        mockMvc.perform(get("/api/v1/users/me/favorites")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ipoId").value(1L))
                .andExpect(jsonPath("$[0].companyName").value("AIPO"))
                .andExpect(jsonPath("$[0].subscriptionPeriod.startDate").value("2026-04-28"))
                .andExpect(jsonPath("$[0].isFavorite").value(true));

        verify(favoriteService).getFavorites(userId);
    }

    @Test
    @DisplayName("관심종목 조회 API는 인증이 필요하다")
    void getFavorites_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/favorites")
                        .with(SecurityMockMvcRequestPostProcessors.anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("응답 JSON은 관심종목 목록 구조를 따른다")
    void getFavorites_returnsExpectedJsonStructure() throws Exception {
        Long userId = 20L;
        CustomUserDetails principal = new CustomUserDetails(userId, "favorite-user", "USER");

        when(favoriteService.getFavorites(userId)).thenReturn(List.of(
                new FavoriteStockResponse(
                        3L,
                        "BIPO",
                        "데이터 플랫폼 기업",
                        new BigDecimal("18000.00"),
                        new DateRange(LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 4)),
                        true,
                        90,
                        "대표주관사",
                        "청약",
                        "05.03 ~ 05.04",
                        true
                )
        ));

        mockMvc.perform(get("/api/v1/users/me/favorites")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ipoId").exists())
                .andExpect(jsonPath("$[0].companyName").exists())
                .andExpect(jsonPath("$[0].oneLineDescription").exists())
                .andExpect(jsonPath("$[0].confirmedOfferPrice").exists())
                .andExpect(jsonPath("$[0].subscriptionPeriod").isMap())
                .andExpect(jsonPath("$[0].isFavorite").value(true));

        verify(favoriteService).getFavorites(userId);
    }

    @Test
    @DisplayName("로그인 사용자는 관심종목을 등록할 수 있다")
    void addFavorite_success() throws Exception {
        Long userId = 10L;
        Long ipoId = 1L;
        CustomUserDetails principal = new CustomUserDetails(userId, "investor", "USER");

        mockMvc.perform(post("/api/v1/users/me/favorites/{ipoId}", ipoId)
                        .with(user(principal)))
                .andExpect(status().isNoContent());

        verify(favoriteService).addFavorite(userId, ipoId);
    }

    @Test
    @DisplayName("없는 종목 등록 시 공통 예외 응답을 반환한다")
    void addFavorite_whenIpoDoesNotExist_returnsErrorResponse() throws Exception {
        Long userId = 10L;
        Long ipoId = 999L;
        CustomUserDetails principal = new CustomUserDetails(userId, "investor", "USER");

        doThrow(new CustomException(ErrorCode.IPO_NOT_FOUND))
                .when(favoriteService).addFavorite(userId, ipoId);

        mockMvc.perform(post("/api/v1/users/me/favorites/{ipoId}", ipoId)
                        .with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IPO_NOT_FOUND"));
    }

    @Test
    @DisplayName("중복 등록 시 공통 예외 응답을 반환한다")
    void addFavorite_whenDuplicate_returnsErrorResponse() throws Exception {
        Long userId = 10L;
        Long ipoId = 1L;
        CustomUserDetails principal = new CustomUserDetails(userId, "investor", "USER");

        doThrow(new CustomException(ErrorCode.DUPLICATE_FAVORITE_STOCK))
                .when(favoriteService).addFavorite(userId, ipoId);

        mockMvc.perform(post("/api/v1/users/me/favorites/{ipoId}", ipoId)
                        .with(user(principal)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_FAVORITE_STOCK"));
    }

    @Test
    @DisplayName("로그인 사용자는 관심종목을 삭제할 수 있다")
    void removeFavorite_success() throws Exception {
        Long userId = 10L;
        Long ipoId = 1L;
        CustomUserDetails principal = new CustomUserDetails(userId, "investor", "USER");

        mockMvc.perform(delete("/api/v1/users/me/favorites/{ipoId}", ipoId)
                        .with(user(principal)))
                .andExpect(status().isNoContent());

        verify(favoriteService).removeFavorite(userId, ipoId);
    }

    @Test
    @DisplayName("없는 관심종목 삭제 시 공통 예외 응답을 반환한다")
    void removeFavorite_whenFavoriteDoesNotExist_returnsErrorResponse() throws Exception {
        Long userId = 10L;
        Long ipoId = 1L;
        CustomUserDetails principal = new CustomUserDetails(userId, "investor", "USER");

        doThrow(new CustomException(ErrorCode.FAVORITE_STOCK_NOT_FOUND))
                .when(favoriteService).removeFavorite(userId, ipoId);

        mockMvc.perform(delete("/api/v1/users/me/favorites/{ipoId}", ipoId)
                        .with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FAVORITE_STOCK_NOT_FOUND"));
    }
}
