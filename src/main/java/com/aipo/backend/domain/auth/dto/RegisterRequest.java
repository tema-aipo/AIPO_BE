package com.aipo.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "회원가입 요청")
public class RegisterRequest {

    @NotBlank
    @Schema(description = "로그인 아이디", example = "investor01")
    private String loginId;

    @NotBlank
    @Schema(description = "비밀번호", example = "password1234")
    private String password;

    @NotBlank
    @Schema(description = "사용자 이름", example = "홍길동")
    private String userName;

    @Schema(description = "이메일", example = "investor01@example.com")
    private String email;
}
