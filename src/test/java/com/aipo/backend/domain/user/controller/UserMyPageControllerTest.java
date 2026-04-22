package com.aipo.backend.domain.user.controller;

import com.aipo.backend.domain.auth.dto.MessageResponse;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileTestStatus;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.user.dto.ChangePasswordRequest;
import com.aipo.backend.domain.user.dto.InvestmentProfileSummaryResponse;
import com.aipo.backend.domain.user.dto.MyPageResponse;
import com.aipo.backend.domain.user.dto.NotificationSettingResponse;
import com.aipo.backend.domain.user.dto.UpdateNotificationSettingRequest;
import com.aipo.backend.domain.user.dto.UpdateUserProfileRequest;
import com.aipo.backend.domain.user.dto.UserProfileResponse;
import com.aipo.backend.domain.user.dto.WithdrawRequest;
import com.aipo.backend.domain.user.service.MyPageService;
import com.aipo.backend.domain.user.service.NotificationSettingService;
import com.aipo.backend.domain.user.service.UserProfileService;
import com.aipo.backend.global.config.SecurityConfig;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserMyPageController.class)
@Import(SecurityConfig.class)
class UserMyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MyPageService myPageService;

    @MockBean
    private NotificationSettingService notificationSettingService;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("마이페이지 기본 조회가 정상 동작한다")
    void getMyPage_success() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(1L, "demo-user", "USER");
        when(myPageService.getMyPage(1L)).thenReturn(new MyPageResponse(
                "홍길동",
                new InvestmentProfileSummaryResponse(
                        InvestmentProfileTestStatus.COMPLETED,
                        InvestmentProfileType.STABLE,
                        "안정형",
                        "변동성보다 안정적인 흐름을 선호하는 성향이에요."
                ),
                new NotificationSettingResponse(true, false),
                true,
                true,
                "문의 준비중",
                "1.0.0"
        ));

        mockMvc.perform(get("/api/v1/users/me/mypage").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("홍길동"))
                .andExpect(jsonPath("$.investmentProfile.profileLabel").value("안정형"))
                .andExpect(jsonPath("$.notifications.listingDateNotificationEnabled").value(false));
    }

    @Test
    @DisplayName("알림 설정 조회와 수정이 정상 동작한다")
    void notifications_success() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(1L, "demo-user", "USER");
        when(notificationSettingService.getSettings(1L)).thenReturn(new NotificationSettingResponse(true, true));
        when(notificationSettingService.updateSettings(1L, new UpdateNotificationSettingRequest(false, true)))
                .thenReturn(new NotificationSettingResponse(false, true));

        mockMvc.perform(get("/api/v1/users/me/notifications").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionScheduleNotificationEnabled").value(true));

        mockMvc.perform(patch("/api/v1/users/me/notifications")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateNotificationSettingRequest(false, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionScheduleNotificationEnabled").value(false))
                .andExpect(jsonPath("$.listingDateNotificationEnabled").value(true));
    }

    @Test
    @DisplayName("내 정보 조회와 수정이 정상 동작한다")
    void profile_success() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(1L, "demo-user", "USER");
        when(userProfileService.getProfile(1L)).thenReturn(new UserProfileResponse(1L, "demo-user", "홍길동", "demo@aipo.test"));
        when(userProfileService.updateProfile(1L, new UpdateUserProfileRequest("김투자", "kim@aipo.test")))
                .thenReturn(new UserProfileResponse(1L, "demo-user", "김투자", "kim@aipo.test"));

        mockMvc.perform(get("/api/v1/users/me/profile").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("demo-user"));

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserProfileRequest("김투자", "kim@aipo.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("김투자"))
                .andExpect(jsonPath("$.email").value("kim@aipo.test"));
    }

    @Test
    @DisplayName("비밀번호 변경 정상과 실패를 검증한다")
    void changePassword_successAndFailure() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(1L, "demo-user", "USER");
        when(userProfileService.changePassword(1L, new ChangePasswordRequest("test1234", "new-password")))
                .thenReturn(new MessageResponse("비밀번호가 변경되었습니다."));
        doThrow(new CustomException(ErrorCode.INVALID_PASSWORD))
                .when(userProfileService).changePassword(1L, new ChangePasswordRequest("wrong", "new-password"));

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest("test1234", "new-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."));

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest("wrong", "new-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
    }

    @Test
    @DisplayName("회원탈퇴 정상과 비밀번호 불일치 실패를 검증한다")
    void withdraw_successAndFailure() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(1L, "demo-user", "USER");
        when(userProfileService.withdraw(1L, "test1234"))
                .thenReturn(new MessageResponse("회원탈퇴가 완료되었습니다."));
        doThrow(new CustomException(ErrorCode.INVALID_PASSWORD))
                .when(userProfileService).withdraw(1L, "wrong");

        mockMvc.perform(post("/api/v1/users/me/withdraw")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest("test1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원탈퇴가 완료되었습니다."));

        mockMvc.perform(post("/api/v1/users/me/withdraw")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest("wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
    }
}
