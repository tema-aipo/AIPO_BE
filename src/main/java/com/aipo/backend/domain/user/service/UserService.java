package com.aipo.backend.domain.user.service;

import com.aipo.backend.domain.auth.repository.UserRefreshTokenRepository;
import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.entity.UserStatus;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getUserStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN_USER);
        }

        user.withdraw();

        userRefreshTokenRepository.deleteByUser_UserId(userId);
    }
}