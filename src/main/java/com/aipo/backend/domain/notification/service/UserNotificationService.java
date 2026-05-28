package com.aipo.backend.domain.notification.service;

import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.entity.UserFavoriteStock;
import com.aipo.backend.domain.ipo.repository.UserFavoriteStockRepository;
import com.aipo.backend.domain.ipo.service.IpoStockViewMapper;
import com.aipo.backend.domain.notification.dto.NotificationItemResponse;
import com.aipo.backend.domain.notification.dto.NotificationListResponse;
import com.aipo.backend.domain.notification.dto.UnreadNotificationCountResponse;
import com.aipo.backend.domain.notification.entity.NotificationType;
import com.aipo.backend.domain.notification.entity.UserNotification;
import com.aipo.backend.domain.notification.repository.UserNotificationRepository;
import com.aipo.backend.domain.user.entity.UserNotificationSetting;
import com.aipo.backend.domain.user.repository.UserNotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserNotificationService {

    private final UserNotificationRepository userNotificationRepository;
    private final UserFavoriteStockRepository userFavoriteStockRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        List<NotificationItemResponse> items = userNotificationRepository
                .findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, PageRequest.of(safePage, safeSize))
                .stream()
                .map(this::toResponse)
                .toList();

        return new NotificationListResponse(items, countUnread(userId));
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(Long userId) {
        return new UnreadNotificationCountResponse(countUnread(userId));
    }

    public void markRead(Long userId, Long notificationId) {
        userNotificationRepository.findByIdAndUserIdAndDeletedFalse(notificationId, userId)
                .ifPresent(UserNotification::markRead);
    }

    public void markAllRead(Long userId) {
        userNotificationRepository.findAllByUserIdAndReadFalseAndDeletedFalse(userId)
                .forEach(UserNotification::markRead);
    }

    public void delete(Long userId, Long notificationId) {
        userNotificationRepository.findByIdAndUserIdAndDeletedFalse(notificationId, userId)
                .ifPresent(UserNotification::delete);
    }

    public int createScheduleNotifications(LocalDate targetDate) {
        List<UserFavoriteStock> favorites = userFavoriteStockRepository.findAll();
        int createdCount = 0;

        for (UserFavoriteStock favorite : favorites) {
            if (!favorite.isNotificationEnabled()) {
                continue;
            }

            UserNotificationSetting setting = userNotificationSettingRepository.findByUserId(favorite.getUserId())
                    .orElse(null);
            IpoStock stock = favorite.getStock();

            if (isSubscriptionEnabled(setting)) {
                LocalDate subscriptionStartDate = IpoStockViewMapper.parseSubscriptionDateText(stock.getSubscriptionDate(), 0);
                LocalDate subscriptionEndDate = IpoStockViewMapper.parseSubscriptionDateText(stock.getSubscriptionDate(), 1);
                if (targetDate.equals(subscriptionStartDate)) {
                    createdCount += createIfAbsent(favorite.getUserId(), stock, NotificationType.SUBSCRIPTION_START, targetDate);
                }
                if (targetDate.equals(subscriptionEndDate)) {
                    createdCount += createIfAbsent(favorite.getUserId(), stock, NotificationType.SUBSCRIPTION_END, targetDate);
                }
            }

            if (isListingEnabled(setting) && targetDate.equals(stock.getListingDate())) {
                createdCount += createIfAbsent(favorite.getUserId(), stock, NotificationType.LISTING_DATE, targetDate);
            }
        }

        return createdCount;
    }

    private int createIfAbsent(Long userId, IpoStock stock, NotificationType type, LocalDate targetDate) {
        if (userNotificationRepository.existsByUserIdAndStock_IdAndNotificationTypeAndTargetDate(
                userId, stock.getId(), type, targetDate
        )) {
            return 0;
        }

        userNotificationRepository.save(UserNotification.create(
                userId,
                stock,
                type,
                title(type),
                content(type, IpoStockViewMapper.displayCompanyName(stock)),
                targetDate
        ));
        return 1;
    }

    private boolean isSubscriptionEnabled(UserNotificationSetting setting) {
        return setting == null || setting.isSubscriptionScheduleNotificationEnabled();
    }

    private boolean isListingEnabled(UserNotificationSetting setting) {
        return setting == null || setting.isListingDateNotificationEnabled();
    }

    private long countUnread(Long userId) {
        return userNotificationRepository.countByUserIdAndReadFalseAndDeletedFalse(userId);
    }

    private NotificationItemResponse toResponse(UserNotification notification) {
        IpoStock stock = notification.getStock();
        return new NotificationItemResponse(
                notification.getId(),
                notification.getNotificationType().name(),
                notification.getTitle(),
                notification.getContent(),
                stock == null ? null : stock.getId(),
                stock == null ? null : stock.getStockCode(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    private String title(NotificationType type) {
        return switch (type) {
            case SUBSCRIPTION_START -> "청약 시작 알림";
            case SUBSCRIPTION_END -> "청약 마감 알림";
            case LISTING_DATE -> "상장일 알림";
        };
    }

    private String content(NotificationType type, String companyName) {
        return switch (type) {
            case SUBSCRIPTION_START -> companyName + " 청약이 오늘 시작됩니다.";
            case SUBSCRIPTION_END -> companyName + " 청약이 오늘 마감됩니다.";
            case LISTING_DATE -> companyName + " 상장일이 오늘입니다.";
        };
    }
}
