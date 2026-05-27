package com.aipo.backend.domain.auth.service;

import com.aipo.backend.domain.auth.dto.MessageResponse;
import com.aipo.backend.domain.auth.entity.UserRefreshToken;
import com.aipo.backend.domain.auth.repository.UserRefreshTokenRepository;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
import com.aipo.backend.domain.log.service.LoginLogService;
import com.aipo.backend.domain.user.service.NotificationSettingService;
import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRefreshTokenRepository userRefreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private InvestmentProfileService investmentProfileService;

    @Mock
    private NotificationSettingService notificationSettingService;

    @Mock
    private LoginLogService loginLogService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("유효한 access token으로 로그아웃하면 refresh token이 있으면 삭제한다")
    void logout_success() {
        User user = user(1L, "demo-user");
        UserRefreshToken refreshToken = new UserRefreshToken(user, "refresh-token", LocalDateTime.now().plusDays(7));

        when(jwtTokenProvider.resolveToken("Bearer access-token")).thenReturn("access-token");
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getLoginId("access-token")).thenReturn("demo-user");
        when(userRepository.findByLoginId("demo-user")).thenReturn(Optional.of(user));
        when(userRefreshTokenRepository.findByUser_UserId(1L)).thenReturn(Optional.of(refreshToken));

        MessageResponse response = authService.logout("Bearer access-token");

        assertThat(response.getMessage()).isEqualTo("로그아웃되었습니다.");
        verify(userRefreshTokenRepository).delete(refreshToken);
    }

    @Test
    @DisplayName("refresh token이 없어도 로그아웃은 성공 처리한다")
    void logout_whenRefreshTokenMissing_stillSucceeds() {
        User user = user(1L, "demo-user");

        when(jwtTokenProvider.resolveToken("Bearer access-token")).thenReturn("access-token");
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getLoginId("access-token")).thenReturn("demo-user");
        when(userRepository.findByLoginId("demo-user")).thenReturn(Optional.of(user));
        when(userRefreshTokenRepository.findByUser_UserId(1L)).thenReturn(Optional.empty());

        MessageResponse response = authService.logout("Bearer access-token");

        assertThat(response.getMessage()).isEqualTo("로그아웃되었습니다.");
        verify(userRefreshTokenRepository, never()).delete(any(UserRefreshToken.class));
    }

    @Test
    @DisplayName("Authorization 헤더 형식이 잘못되면 INVALID_ACCESS_TOKEN 예외가 발생한다")
    void logout_whenAuthorizationHeaderInvalid_throwInvalidAccessToken() {
        when(jwtTokenProvider.resolveToken("access-token")).thenReturn(null);

        assertThatThrownBy(() -> authService.logout("access-token"))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ACCESS_TOKEN);
    }

    @Test
    @DisplayName("토큰에서 loginId 추출에 실패하면 INVALID_ACCESS_TOKEN 예외가 발생한다")
    void logout_whenGetLoginIdFails_throwInvalidAccessToken() {
        when(jwtTokenProvider.resolveToken("Bearer access-token")).thenReturn("access-token");
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getLoginId("access-token")).thenThrow(new JwtException("invalid"));

        assertThatThrownBy(() -> authService.logout("Bearer access-token"))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ACCESS_TOKEN);
    }

    private User user(Long userId, String loginId) {
        User user = instantiate(User.class);
        ReflectionTestUtils.setField(user, "userId", userId);
        ReflectionTestUtils.setField(user, "loginId", loginId);
        return user;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }
}
