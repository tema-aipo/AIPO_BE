package com.aipo.backend.domain.auth.service;

import com.aipo.backend.domain.auth.dto.*;
import com.aipo.backend.domain.auth.entity.UserRefreshToken;
import com.aipo.backend.domain.auth.repository.UserRefreshTokenRepository;
import com.aipo.backend.domain.log.service.LoginLogService;
import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginLogService loginLogService;

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        User user = new User(
                request.getLoginId(),
                passwordEncoder.encode(request.getPassword()),
                request.getUserName(),
                request.getEmail()
        );

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getUserId(),
                savedUser.getLoginId(),
                savedUser.getUserName(),
                savedUser.getEmail()
        );
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getLoginId(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updateLastLoginAt();
        loginLogService.record(user);

        String accessToken = jwtTokenProvider.createAccessToken(user.getLoginId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getLoginId());

        userRefreshTokenRepository.deleteByUser_UserId(user.getUserId());
        userRefreshTokenRepository.save(
                new UserRefreshToken(
                        user,
                        refreshToken,
                        jwtTokenProvider.getRefreshTokenExpiryDate()
                )
        );

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                user.getUserId(),
                user.getLoginId(),
                user.getUserName()
        );
    }

    @Transactional(readOnly = true)
    public ReissueResponse reissue(ReissueRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 refresh token입니다.");
        }

        UserRefreshToken savedToken = userRefreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("저장된 refresh token이 없습니다."));

        String loginId = jwtTokenProvider.getLoginId(refreshToken);

        String newAccessToken = jwtTokenProvider.createAccessToken(loginId);

        return new ReissueResponse(newAccessToken, "Bearer");
    }

    public MessageResponse logout(String bearerToken) {
        String accessToken = jwtTokenProvider.resolveToken(bearerToken);

        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
            throw new IllegalArgumentException("유효하지 않은 access token입니다.");
        }

        String loginId = jwtTokenProvider.getLoginId(accessToken);
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        userRefreshTokenRepository.deleteByUser_UserId(user.getUserId());

        return new MessageResponse("로그아웃되었습니다.");
    }
}
// NOTE:
// 현재 AuthService는 최소 인증 플로우(register/login/reissue/logout) 중심이다.
// 추후 사용자 상태 검증, 로그인 실패 횟수 제한, 감사 로그,
// 세분화된 예외 처리 및 관리자 인증 정책을 추가할 수 있다.