package org.study.jibeo.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import team.themoment.sdk.exception.ExpectedException;

@Getter
public enum ErrorCode {

    DUPLICATE_LOGIN_ID(HttpStatus.BAD_REQUEST, "이미 사용 중인 아이디입니다."),
    COMPANY_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 회사입니다."),
    ROLE_COMPANY_MISMATCH(HttpStatus.BAD_REQUEST, "회사 타입과 역할이 맞지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 사용자입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public ExpectedException toException() {
        return new ExpectedException(message, status);
    }
}