package com.aipo.backend.domain.auth.controller;

import com.aipo.backend.domain.auth.dto.LoginResponse;
import com.aipo.backend.domain.auth.dto.MessageResponse;
import com.aipo.backend.domain.auth.service.AuthService;
import com.aipo.backend.global.config.SecurityConfig;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("로그아웃 요청에 Authorization 헤더가 있으면 정상 응답을 반환한다")
    void logout_success() throws Exception {
        when(authService.logout("Bearer access-token")).thenReturn(new MessageResponse("로그아웃되었습니다."));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));
    }

    @Test
    @DisplayName("로그아웃 요청에 Authorization 헤더가 없어도 500이 아닌 INVALID_ACCESS_TOKEN 응답을 반환한다")
    void logout_withoutAuthorizationHeader_returnsUnauthorized() throws Exception {
        when(authService.logout(null)).thenThrow(new CustomException(ErrorCode.INVALID_ACCESS_TOKEN));

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }
}
