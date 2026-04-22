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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRefreshTokenRepository userRefreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    @DisplayName("내 정보 조회 시 이름, 아이디, 이메일을 반환한다")
    void getProfile_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "demo-user", "홍길동", "demo@aipo.test")));

        UserProfileResponse response = userProfileService.getProfile(1L);

        assertThat(response.loginId()).isEqualTo("demo-user");
        assertThat(response.userName()).isEqualTo("홍길동");
        assertThat(response.email()).isEqualTo("demo@aipo.test");
    }

    @Test
    @DisplayName("내 정보 수정 시 이름과 이메일을 변경한다")
    void updateProfile_success() {
        User user = user(1L, "demo-user", "홍길동", "demo@aipo.test");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndUserIdNot("updated@aipo.test", 1L)).thenReturn(false);

        UserProfileResponse response = userProfileService.updateProfile(
                1L,
                new UpdateUserProfileRequest("김투자", "updated@aipo.test")
        );

        assertThat(response.userName()).isEqualTo("김투자");
        assertThat(response.email()).isEqualTo("updated@aipo.test");
    }

    @Test
    @DisplayName("비밀번호 변경 시 현재 비밀번호가 맞으면 변경 후 refresh token을 삭제한다")
    void changePassword_success() {
        User user = user(1L, "demo-user", "홍길동", "demo@aipo.test");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");

        MessageResponse response = userProfileService.changePassword(
                1L,
                new ChangePasswordRequest("old-password", "new-password")
        );

        assertThat(response.getMessage()).isEqualTo("비밀번호가 변경되었습니다.");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-new-password");
        verify(userRefreshTokenRepository).deleteByUser_UserId(1L);
    }

    @Test
    @DisplayName("비밀번호 변경 시 현재 비밀번호가 틀리면 INVALID_PASSWORD 예외가 발생한다")
    void changePassword_whenCurrentPasswordInvalid_throwsException() {
        User user = user(1L, "demo-user", "홍길동", "demo@aipo.test");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userProfileService.changePassword(
                1L,
                new ChangePasswordRequest("wrong-password", "new-password")
        ))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("회원탈퇴 시 비밀번호가 맞으면 WITHDRAWN 처리 후 refresh token을 삭제한다")
    void withdraw_success() {
        User user = user(1L, "demo-user", "홍길동", "demo@aipo.test");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("test1234", user.getPasswordHash())).thenReturn(true);

        MessageResponse response = userProfileService.withdraw(1L, "test1234");

        assertThat(response.getMessage()).isEqualTo("회원탈퇴가 완료되었습니다.");
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.WITHDRAWN);
        verify(userRefreshTokenRepository).deleteByUser_UserId(1L);
    }

    @Test
    @DisplayName("회원탈퇴 시 비밀번호가 틀리면 INVALID_PASSWORD 예외가 발생한다")
    void withdraw_whenPasswordInvalid_throwsException() {
        User user = user(1L, "demo-user", "홍길동", "demo@aipo.test");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userProfileService.withdraw(1L, "wrong-password"))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    private User user(Long userId, String loginId, String userName, String email) {
        User user = new User(loginId, "encoded-password", userName, email);
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }
}
