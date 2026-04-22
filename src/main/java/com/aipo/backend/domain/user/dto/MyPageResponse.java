package com.aipo.backend.domain.user.dto;

public record MyPageResponse(
        String userName,
        InvestmentProfileSummaryResponse investmentProfile,
        NotificationSettingResponse notifications,
        boolean logoutSupported,
        boolean withdrawSupported,
        String inquiryChannel,
        String appVersion
) {
}
