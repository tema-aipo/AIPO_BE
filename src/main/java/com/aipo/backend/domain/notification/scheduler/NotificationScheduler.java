package com.aipo.backend.domain.notification.scheduler;

import com.aipo.backend.domain.notification.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final UserNotificationService userNotificationService;

    @Scheduled(cron = "${notification.schedule.cron:0 0 8 * * *}", zone = "Asia/Seoul")
    public void createDailyScheduleNotifications() {
        LocalDate today = LocalDate.now(KOREA_ZONE);
        int createdCount = userNotificationService.createScheduleNotifications(today);
        log.info("[Notification Scheduler] created {} notifications for {}", createdCount, today);
    }
}
