package com.aipo.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 아이디 사용 가능 여부 응답")
public record LoginIdAvailabilityResponse(
        @Schema(description = "확인한 로그인 아이디", example = "investor01")
        String loginId,
        @Schema(description = "사용 가능 여부", example = "true")
        boolean available
) {
}
