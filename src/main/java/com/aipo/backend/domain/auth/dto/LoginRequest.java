package com.aipo.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "로그인 요청")
public class LoginRequest {

    @NotBlank
    @Schema(description = "로그인 아이디", example = "investor01")
    private String loginId;

    @NotBlank
    @Schema(description = "비밀번호", example = "password1234")
    private String password;
}
