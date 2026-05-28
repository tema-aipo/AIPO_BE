package com.aipo.backend.domain.notification.repository;

import com.aipo.backend.domain.notification.entity.NotificationType;
import com.aipo.backend.domain.notification.entity.UserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    @EntityGraph(attributePaths = "stock")
    List<UserNotification> findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<UserNotification> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    long countByUserIdAndReadFalseAndDeletedFalse(Long userId);

    List<UserNotification> findAllByUserIdAndReadFalseAndDeletedFalse(Long userId);

    boolean existsByUserIdAndStock_IdAndNotificationTypeAndTargetDate(
            Long userId,
            Long stockId,
            NotificationType notificationType,
            LocalDate targetDate
    );
}
