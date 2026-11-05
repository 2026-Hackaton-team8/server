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
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 사용자입니다."),
    COMPANY_CHOICE_REQUIRED(HttpStatus.BAD_REQUEST, "회사를 선택하거나 새 회사 정보를 입력하세요. 둘 중 하나만 입력할 수 있습니다."),
    DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "이미 등록된 사업자등록번호입니다."),
    MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 모델입니다."),
    DUPLICATE_MODEL_CODE(HttpStatus.CONFLICT, "이미 등록된 모델 코드입니다."),
    DUPLICATE_PART_CODE(HttpStatus.CONFLICT, "이 모델에 이미 등록된 부품 코드입니다."),
    EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 설비입니다.");

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