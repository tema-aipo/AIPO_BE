package com.aipo.backend.domain.user.service;

import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileResultResponse;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileTestStatus;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
import com.aipo.backend.domain.user.dto.MyPageResponse;
import com.aipo.backend.domain.user.dto.NotificationSettingResponse;
import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvestmentProfileService investmentProfileService;

    @Mock
    private NotificationSettingService notificationSettingService;

    @InjectMocks
    private MyPageService myPageService;

    @Test
    @DisplayName("마이페이지 기본 조회 시 사용자명, 투자성향 요약, 알림 설정을 함께 반환한다")
    void getMyPage_success() {
        ReflectionTestUtils.setField(myPageService, "appVersion", "1.0.0");
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User("demo-user", "encoded", "홍길동", "demo@aipo.test")));
        when(investmentProfileService.getCurrentResult(1L)).thenReturn(new InvestmentProfileResultResponse(
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
                10,
                null
        ));
        when(notificationSettingService.getSettings(1L)).thenReturn(new NotificationSettingResponse(true, false));

        MyPageResponse response = myPageService.getMyPage(1L);

        assertThat(response.userName()).isEqualTo("홍길동");
        assertThat(response.investmentProfile().profileLabel()).isEqualTo("중립형");
        assertThat(response.notifications().listingDateNotificationEnabled()).isFalse();
        assertThat(response.logoutSupported()).isTrue();
    }
}
