package com.aipo.backend.domain.user.service;

import com.aipo.backend.domain.user.dto.NotificationSettingResponse;
import com.aipo.backend.domain.user.dto.UpdateNotificationSettingRequest;
import com.aipo.backend.domain.user.entity.UserNotificationSetting;
import com.aipo.backend.domain.user.repository.UserNotificationSettingRepository;
import com.aipo.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceTest {

    @Mock
    private UserNotificationSettingRepository userNotificationSettingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationSettingService notificationSettingService;

    @Test
    @DisplayName("알림 설정 조회 시 기존 설정을 반환한다")
    void getSettings_success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userNotificationSettingRepository.findByUserId(1L))
                .thenReturn(Optional.of(setting(1L, true, false)));

        NotificationSettingResponse response = notificationSettingService.getSettings(1L);

        assertThat(response.subscriptionScheduleNotificationEnabled()).isTrue();
        assertThat(response.listingDateNotificationEnabled()).isFalse();
    }

    @Test
    @DisplayName("알림 설정이 없으면 기본 설정을 생성한다")
    void getSettings_whenMissing_createsDefault() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userNotificationSettingRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userNotificationSettingRepository.save(any(UserNotificationSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationSettingResponse response = notificationSettingService.getSettings(1L);

        assertThat(response.subscriptionScheduleNotificationEnabled()).isTrue();
        assertThat(response.listingDateNotificationEnabled()).isTrue();
    }

    @Test
    @DisplayName("알림 설정 수정 시 두 토글 값이 변경된다")
    void updateSettings_success() {
        UserNotificationSetting setting = setting(1L, true, true);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userNotificationSettingRepository.findByUserId(1L)).thenReturn(Optional.of(setting));

        NotificationSettingResponse response = notificationSettingService.updateSettings(
                1L,
                new UpdateNotificationSettingRequest(false, true)
        );

        assertThat(response.subscriptionScheduleNotificationEnabled()).isFalse();
        assertThat(response.listingDateNotificationEnabled()).isTrue();
        verify(userNotificationSettingRepository).findByUserId(1L);
    }

    private UserNotificationSetting setting(Long userId, boolean subscriptionEnabled, boolean listingEnabled) {
        UserNotificationSetting setting = UserNotificationSetting.createDefault(userId);
        setting.update(subscriptionEnabled, listingEnabled);
        ReflectionTestUtils.setField(setting, "id", 1L);
        return setting;
    }
}
