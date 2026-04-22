package com.aipo.backend.domain.user.service;

import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileResultResponse;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
import com.aipo.backend.domain.user.dto.InvestmentProfileSummaryResponse;
import com.aipo.backend.domain.user.dto.MyPageResponse;
import com.aipo.backend.domain.user.dto.NotificationSettingResponse;
import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepository;
    private final InvestmentProfileService investmentProfileService;
    private final NotificationSettingService notificationSettingService;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    public MyPageResponse getMyPage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        InvestmentProfileResultResponse result = investmentProfileService.getCurrentResult(userId);
        NotificationSettingResponse notifications = notificationSettingService.getSettings(userId);

        return new MyPageResponse(
                user.getUserName(),
                new InvestmentProfileSummaryResponse(
                        result.testStatus(),
                        result.profileType(),
                        result.profileLabel(),
                        result.description()
                ),
                notifications,
                true,
                true,
                "문의 준비중",
                appVersion
        );
    }
}
