package com.aipo.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "토큰 재발급 요청")
public class ReissueRequest {

    @NotBlank
    @Schema(description = "로그인 또는 이전 재발급에서 받은 refresh token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
