package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.entity.UserRole;
import com.aipo.backend.domain.user.entity.UserStatus;
import com.aipo.backend.domain.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    public Page<UserSummaryResponse> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAllByRoleAndFilter(UserRole.USER, keyword, status, pageable)
                .map(UserSummaryResponse::from);
    }

    @GetMapping("/{userId}")
    public UserDetailResponse getUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return UserDetailResponse.from(user);
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserDetailResponse> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStatusRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getRole() == UserRole.ADMIN) {
            throw new BadCredentialsException("관리자 계정의 상태는 변경할 수 없습니다.");
        }

        user.updateStatus(request.getStatus());
        userRepository.save(user);

        return ResponseEntity.ok(UserDetailResponse.from(user));
    }

    @Getter
    @AllArgsConstructor
    static class UserSummaryResponse {
        private Long userId;
        private String loginId;
        private String userName;
        private String email;
        private UserStatus userStatus;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;

        static UserSummaryResponse from(User user) {
            return new UserSummaryResponse(
                    user.getUserId(),
                    user.getLoginId(),
                    user.getUserName(),
                    user.getEmail(),
                    user.getUserStatus(),
                    user.getCreatedAt(),
                    user.getLastLoginAt()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    static class UserDetailResponse {
        private Long userId;
        private String loginId;
        private String userName;
        private String email;
        private UserRole role;
        private UserStatus userStatus;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastLoginAt;
        private LocalDateTime passwordUpdatedAt;
        private LocalDateTime deletedAt;

        static UserDetailResponse from(User user) {
            return new UserDetailResponse(
                    user.getUserId(),
                    user.getLoginId(),
                    user.getUserName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getUserStatus(),
                    user.getCreatedAt(),
                    user.getUpdatedAt(),
                    user.getLastLoginAt(),
                    user.getPasswordUpdatedAt(),
                    user.getDeletedAt()
            );
        }
    }

    @Getter
    static class UpdateStatusRequest {
        @NotNull(message = "변경할 상태 값은 필수입니다.")
        private UserStatus status;
    }
}
