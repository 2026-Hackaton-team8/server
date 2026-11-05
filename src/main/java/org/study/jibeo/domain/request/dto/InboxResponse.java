package org.study.jibeo.domain.request.dto;

import java.util.List;
import java.util.Map;

/**
 * 호출 수신함. counts 는 탭별 개수 (ALL, RECEIVED, ASSIGNED, COMPLETED, SELF_RESOLVED)
 */
public record InboxResponse(
        Map<String, Long> counts,
        List<RequestSummaryResponse> content,
        int page,
        int size,
        long totalElements
) {
}
