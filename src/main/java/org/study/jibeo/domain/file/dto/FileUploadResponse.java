package org.study.jibeo.domain.file.dto;

/** fileKey 는 호출 제출(attachmentKeys)에 쓰고, url 은 미리보기용 서버 주소 (유효 시간 10분 이상, 로그인 없이 열림) */
public record FileUploadResponse(String fileKey, String contentType, long size, String url) {
}
