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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserNotificationService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd");
    private static final int UPCOMING_NOTIFICATION_DAYS = 14;
    private static final int PAST_NOTIFICATION_VISIBLE_DAYS = 1;

    private final UserNotificationRepository userNotificationRepository;
    private final UserFavoriteStockRepository userFavoriteStockRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        LocalDate visibleFrom = LocalDate.now(KOREA_ZONE).minusDays(PAST_NOTIFICATION_VISIBLE_DAYS);

        List<NotificationItemResponse> items = userNotificationRepository
                .findAllByUserIdAndDeletedFalseAndTargetDateGreaterThanEqualOrderByCreatedAtDesc(
                        userId,
                        visibleFrom,
                        PageRequest.of(safePage, safeSize)
                )
                .stream()
                .map(this::toResponse)
                .toList();

        return new NotificationListResponse(items, countVisibleUnread(userId));
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(Long userId) {
        return new UnreadNotificationCountResponse(countVisibleUnread(userId));
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
            createdCount += createUpcomingNotifications(favorite, targetDate);
        }

        return createdCount;
    }

    public int createUpcomingNotificationsForFavorite(UserFavoriteStock favorite) {
        return createUpcomingNotifications(favorite, LocalDate.now(KOREA_ZONE));
    }

    private int createUpcomingNotifications(UserFavoriteStock favorite, LocalDate baseDate) {
        if (!favorite.isNotificationEnabled()) {
            return 0;
        }

        UserNotificationSetting setting = userNotificationSettingRepository.findByUserId(favorite.getUserId())
                .orElse(null);
        IpoStock stock = favorite.getStock();
        int createdCount = 0;

        if (isSubscriptionEnabled(setting)) {
            LocalDate subscriptionStartDate = IpoStockViewMapper.parseSubscriptionDateText(stock.getSubscriptionDate(), 0);
            LocalDate subscriptionEndDate = IpoStockViewMapper.parseSubscriptionDateText(stock.getSubscriptionDate(), 1);
            if (isInUpcomingWindow(baseDate, subscriptionStartDate)) {
                createdCount += createIfAbsent(favorite.getUserId(), stock, NotificationType.SUBSCRIPTION_START, subscriptionStartDate);
            }
            if (isInUpcomingWindow(baseDate, subscriptionEndDate)) {
                createdCount += createIfAbsent(favorite.getUserId(), stock, NotificationType.SUBSCRIPTION_END, subscriptionEndDate);
            }
        }

        if (isListingEnabled(setting) && isInUpcomingWindow(baseDate, stock.getListingDate())) {
            createdCount += createIfAbsent(favorite.getUserId(), stock, NotificationType.LISTING_DATE, stock.getListingDate());
        }

        return createdCount;
    }

    private boolean isInUpcomingWindow(LocalDate baseDate, LocalDate targetDate) {
        if (targetDate == null) {
            return false;
        }
        LocalDate until = baseDate.plusDays(UPCOMING_NOTIFICATION_DAYS);
        return !targetDate.isBefore(baseDate) && !targetDate.isAfter(until);
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
                content(type, IpoStockViewMapper.displayCompanyName(stock), targetDate),
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

    private long countVisibleUnread(Long userId) {
        LocalDate visibleFrom = LocalDate.now(KOREA_ZONE).minusDays(PAST_NOTIFICATION_VISIBLE_DAYS);
        return userNotificationRepository.countByUserIdAndReadFalseAndDeletedFalseAndTargetDateGreaterThanEqual(
                userId,
                visibleFrom
        );
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
                notification.getTargetDate(),
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

    private String content(NotificationType type, String companyName, LocalDate targetDate) {
        String targetDateText = targetDate.format(DISPLAY_DATE_FORMATTER);
        return switch (type) {
            case SUBSCRIPTION_START -> companyName + " 청약 시작일은 " + targetDateText + "입니다.";
            case SUBSCRIPTION_END -> companyName + " 청약 마감일은 " + targetDateText + "입니다.";
            case LISTING_DATE -> companyName + " 상장 예정일은 " + targetDateText + "입니다.";
        };
    }
}
