package org.study.jibeo.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 값이 유효하지 않습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.BAD_REQUEST, "DUPLICATE_LOGIN_ID", "이미 사용 중인 아이디입니다."),
    COMPANY_NOT_FOUND(HttpStatus.BAD_REQUEST, "COMPANY_NOT_FOUND", "존재하지 않는 회사입니다."),
    ROLE_COMPANY_MISMATCH(HttpStatus.BAD_REQUEST, "ROLE_COMPANY_MISMATCH", "회사 타입과 역할이 맞지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "리프레시 토큰이 유효하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "데이터 무결성 제약을 위반했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
