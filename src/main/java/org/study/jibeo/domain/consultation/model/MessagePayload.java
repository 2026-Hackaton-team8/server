package org.study.jibeo.domain.consultation.model;

import org.study.jibeo.domain.equipmentmodel.dto.PartResponse;
import org.study.jibeo.domain.request.model.CallReason;
import org.study.jibeo.global.ai.AiDtos;

import java.util.List;

/**
 * AI 메시지의 구조화된 내용 (consultation_message.payload, jsonb).
 * QUESTION: options, candidates · SOLUTION: steps, cautions, evidence, parts · ESCALATE: reason
 * parts = 해결책이 가리키는 부품 (code 로 3D 모델의 부품과 맞춘다)
 */
public record MessagePayload(
        List<String> options,
        List<AiDtos.Candidate> candidates,
        List<String> steps,
        List<String> cautions,
        AiDtos.Evidence evidence,
        CallReason reason,
        List<PartResponse> parts
) {
}
