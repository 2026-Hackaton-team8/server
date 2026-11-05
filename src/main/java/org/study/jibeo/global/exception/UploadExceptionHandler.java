package org.study.jibeo.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import team.themoment.sdk.response.CommonApiResponse;

/** 업로드 크기 초과(spring.servlet.multipart.max-file-size)와 file 파트 누락을 SDK 형식으로 응답 */
@RestControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public CommonApiResponse<Void> handleTooLarge(MaxUploadSizeExceededException e) {
        return new CommonApiResponse<>(HttpStatus.CONTENT_TOO_LARGE, HttpStatus.CONTENT_TOO_LARGE.value(),
                "파일은 20MB까지 올릴 수 있습니다.", null);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public CommonApiResponse<Void> handleMissingPart(MissingServletRequestPartException e) {
        return new CommonApiResponse<>(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value(),
                "'" + e.getRequestPartName() + "' 파일이 필요합니다.", null);
    }
}