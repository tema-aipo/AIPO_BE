package com.aipo.backend.domain.user.dto;

public record UserProfileResponse(
        Long userId,
        String loginId,
        String userName,
        String email
) {
}
