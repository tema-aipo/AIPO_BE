package com.aipo.backend.domain.auth.service;

import com.aipo.backend.domain.auth.dto.*;
import com.aipo.backend.domain.auth.entity.UserRefreshToken;
import com.aipo.backend.domain.auth.repository.UserRefreshTokenRepository;
import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.entity.UserStatus;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
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
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getLoginId(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getUserStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }

        user.updateLastLoginAt();

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getLoginId(),
                user.getRole().name()
        );
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
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        UserRefreshToken savedToken = userRefreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        String loginId = jwtTokenProvider.getLoginId(refreshToken);

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getUserStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getLoginId(),
                user.getRole().name()
        );

        return new ReissueResponse(newAccessToken, "Bearer");
    }

    public MessageResponse logout(String bearerToken) {
        String accessToken = jwtTokenProvider.resolveToken(bearerToken);

        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        String loginId = jwtTokenProvider.getLoginId(accessToken);
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        userRefreshTokenRepository.deleteByUser_UserId(user.getUserId());

        return new MessageResponse("로그아웃되었습니다.");
    }
}