package com.aipo.backend.domain.user.service;

import com.aipo.backend.domain.user.dto.NotificationSettingResponse;
import com.aipo.backend.domain.user.dto.UpdateNotificationSettingRequest;
import com.aipo.backend.domain.user.entity.UserNotificationSetting;
import com.aipo.backend.domain.user.repository.UserNotificationSettingRepository;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSettingService {

    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final UserRepository userRepository;

    public void initializeDefaultSettings(Long userId) {
        validateUser(userId);
        if (userNotificationSettingRepository.existsByUserId(userId)) {
            return;
        }
        userNotificationSettingRepository.save(UserNotificationSetting.createDefault(userId));
    }

    @Transactional(readOnly = true)
    public NotificationSettingResponse getSettings(Long userId) {
        return toResponse(getOrCreate(userId));
    }

    public NotificationSettingResponse updateSettings(Long userId, UpdateNotificationSettingRequest request) {
        UserNotificationSetting setting = getOrCreate(userId);
        setting.update(
                request.subscriptionScheduleNotificationEnabled(),
                request.listingDateNotificationEnabled()
        );
        return toResponse(setting);
    }

    private UserNotificationSetting getOrCreate(Long userId) {
        validateUser(userId);
        return userNotificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> userNotificationSettingRepository.save(UserNotificationSetting.createDefault(userId)));
    }

    private NotificationSettingResponse toResponse(UserNotificationSetting setting) {
        return new NotificationSettingResponse(
                setting.isSubscriptionScheduleNotificationEnabled(),
                setting.isListingDateNotificationEnabled()
        );
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
