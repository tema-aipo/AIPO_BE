package com.aipo.backend.domain.user.service;

import com.aipo.backend.domain.auth.dto.MessageResponse;
import com.aipo.backend.domain.auth.repository.UserRefreshTokenRepository;
import com.aipo.backend.domain.user.dto.ChangePasswordRequest;
import com.aipo.backend.domain.user.dto.UpdateUserProfileRequest;
import com.aipo.backend.domain.user.dto.UserProfileResponse;
import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.entity.UserStatus;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return toResponse(getActiveUser(userId));
    }

    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = getActiveUser(userId);
        String normalizedEmail = normalizeEmail(request.email());
        if (normalizedEmail != null && userRepository.existsByEmailAndUserIdNot(normalizedEmail, userId)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        user.updateProfile(request.userName().trim(), normalizedEmail);
        return toResponse(user);
    }

    public MessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = getActiveUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRefreshTokenRepository.deleteByUser_UserId(userId);
        return new MessageResponse("비밀번호가 변경되었습니다.");
    }

    public MessageResponse withdraw(Long userId, String password) {
        User user = getActiveUser(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
        user.withdraw();
        userRefreshTokenRepository.deleteByUser_UserId(userId);
        return new MessageResponse("회원탈퇴가 완료되었습니다.");
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.getUserStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN_USER);
        }
        return user;
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getUserName(),
                user.getEmail()
        );
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
