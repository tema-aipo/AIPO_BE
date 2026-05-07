package com.aipo.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "로그인 응답")
public class LoginResponse {
    @Schema(description = "API 호출에 사용할 access token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;
    @Schema(description = "access token 재발급에 사용할 refresh token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
    @Schema(description = "토큰 타입. Authorization 헤더에는 Bearer prefix를 사용합니다.", example = "Bearer")
    private String tokenType;
    @Schema(description = "사용자 식별자", example = "1")
    private Long userId;
    @Schema(description = "로그인 아이디", example = "investor01")
    private String loginId;
    @Schema(description = "사용자 이름", example = "홍길동")
    private String userName;
    @Schema(description = "이메일", example = "investor01@aipo.com")
    private String email;
}
