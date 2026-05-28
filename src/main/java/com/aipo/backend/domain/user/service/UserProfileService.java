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
    private final jakarta.persistence.EntityManager entityManager;

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

        // 1. 대화방 관련 피드백 삭제
        entityManager.createNativeQuery("DELETE FROM chat_feedback WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        // 2. 대화방 관련 메시지 삭제
        entityManager.createNativeQuery("DELETE FROM chat_message WHERE chat_session_id IN (SELECT chat_session_id FROM chat_session WHERE user_id = :userId)")
                .setParameter("userId", userId).executeUpdate();

        // 3. 대화 세션 삭제
        entityManager.createNativeQuery("DELETE FROM chat_session WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        // 4. 추천 질문 타겟 유저 삭제
        entityManager.createNativeQuery("DELETE FROM chat_recommended_question WHERE target_user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        // 5. 관심 공모주 목록 삭제
        entityManager.createNativeQuery("DELETE FROM user_favorite_stock WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        // 6. 조회 로그 삭제
        entityManager.createNativeQuery("DELETE FROM ipo_view_log WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        // 7. 투자 성향 답변 삭제
        entityManager.createNativeQuery("DELETE FROM user_investment_profile_answer WHERE result_id IN (SELECT result_id FROM user_investment_profile_result WHERE user_id = :userId)")
                .setParameter("userId", userId).executeUpdate();

        // 8. 투자 성향 분석 결과 삭제
        entityManager.createNativeQuery("DELETE FROM user_investment_profile_result WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        // 9. 챗봇 로그는 보존하되 탈퇴 사용자 참조를 제거
        entityManager.createNativeQuery("UPDATE chatbot_log SET user_id = NULL WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        // 10. 로그인 로그와 투자 성향 요약 삭제
        entityManager.createNativeQuery("DELETE FROM user_login_log WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM user_investment_type WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        // 11. 푸시 알림 설정 삭제
        entityManager.createNativeQuery("DELETE FROM user_notification_setting WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        // 12. 리프레시 토큰 삭제
        entityManager.createNativeQuery("DELETE FROM user_refresh_token WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        // 13. 최종 회원 물리 삭제 (JPA Repository delete 호출로 1차 캐시와 상태 일치시킴!)
        userRepository.delete(user);

        // 즉시 데이터베이스 동기화 및 영속성 컨텍스트 초기화 (Dirty Checking 방지)
        entityManager.flush();
        entityManager.clear();

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
