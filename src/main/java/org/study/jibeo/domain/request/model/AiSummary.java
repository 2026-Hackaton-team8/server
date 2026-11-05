package org.study.jibeo.domain.request.model;

import jakarta.validation.constraints.Size;

/**
 * 호출 신고서의 AI 상담 요약. 사용자가 신고서에서 고칠 수 있어 요청 값으로 받는다. jsonb 컬럼
 */
public record AiSummary(
        @Size(max = 500) String symptom,
        @Size(max = 500) String cause,
        @Size(max = 1000) String triedAction
) {
}
