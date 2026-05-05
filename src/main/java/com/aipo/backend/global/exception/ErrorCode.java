package com.aipo.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_001", "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "만료된 토큰입니다."),
    NOT_ADMIN(HttpStatus.FORBIDDEN, "AUTH_004", "관리자 계정이 아닙니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_005", "접근 권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 아이디입니다."),
    ADMIN_STATUS_CHANGE_DENIED(HttpStatus.BAD_REQUEST, "USER_003", "관리자 계정의 상태는 변경할 수 없습니다."),

    // Document
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOC_001", "문서를 찾을 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "DOC_002", "파일 업로드에 실패했습니다."),

    // Pipeline
    PIPELINE_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "PIPE_001", "파이프라인 작업을 찾을 수 없습니다."),
    PIPELINE_JOB_CANCEL_DENIED(HttpStatus.BAD_REQUEST, "PIPE_002", "취소할 수 없는 상태입니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "요청 값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
