package com.aipo.backend.domain.user.controller;

import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.entity.UserRole;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.domain.user.service.UserService;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import com.aipo.backend.global.security.service.CustomUserDetails;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = userRepository.findByLoginId(principal.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new MeResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getUserName(),
                user.getEmail(),
                user.getRole()
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = userRepository.findByLoginId(principal.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        userService.withdraw(user.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Getter
    @AllArgsConstructor
    static class MeResponse {
        private Long userId;
        private String loginId;
        private String userName;
        private String email;
        private UserRole role;
    }
}