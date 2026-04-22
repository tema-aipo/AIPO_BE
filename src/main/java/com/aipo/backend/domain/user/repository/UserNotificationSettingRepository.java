package com.aipo.backend.domain.user.repository;

import com.aipo.backend.domain.user.entity.UserNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, Long> {

    Optional<UserNotificationSetting> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
