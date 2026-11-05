package org.study.jibeo.domain.consultation.model;

import org.study.jibeo.domain.request.model.CallReason;
import org.study.jibeo.global.ai.AiDtos;

import java.util.List;

/**
 * AI 메시지의 구조화된 내용 (consultation_message.payload, jsonb).
 * QUESTION: options, candidates · SOLUTION: steps, cautions, evidence · ESCALATE: reason
 */
public record MessagePayload(
        List<String> options,
        List<AiDtos.Candidate> candidates,
        List<String> steps,
        List<String> cautions,
        AiDtos.Evidence evidence,
        CallReason reason
) {
}
