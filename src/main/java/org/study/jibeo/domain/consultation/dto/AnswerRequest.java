package org.study.jibeo.domain.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** AI 질문에 대한 답 (선택지를 고르면 그 글을 그대로 보낸다) */
public record AnswerRequest(
        @NotBlank @Size(max = 1000) String content
) {
}
