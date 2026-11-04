package org.study.jibeo.global.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import team.themoment.sdk.response.CommonApiResponse;

import java.util.List;

@RestControllerAdvice
public class ValidationExceptionHandler {

    private static final String MESSAGE = "요청 값이 유효하지 않습니다.";

    public record FieldError(String field, String reason) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonApiResponse<List<FieldError>> handleBody(MethodArgumentNotValidException e) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return badRequest(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public CommonApiResponse<List<FieldError>> handleParam(ConstraintViolationException e) {
        List<FieldError> errors = e.getConstraintViolations().stream()
                .map(v -> new FieldError(lastNode(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        return badRequest(errors);
    }

    private static CommonApiResponse<List<FieldError>> badRequest(List<FieldError> errors) {
        return new CommonApiResponse<>(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value(), MESSAGE, errors);
    }

    // "checkLoginId.loginId" -> "loginId"
    private static String lastNode(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }
}