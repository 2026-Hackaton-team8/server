package org.study.jibeo.domain.consultation.dto;

import org.study.jibeo.domain.consultation.model.ConsultationMessage;
import org.study.jibeo.domain.consultation.model.MessagePayload;
import org.study.jibeo.domain.consultation.model.MessageSender;
import org.study.jibeo.domain.consultation.model.MessageType;

import java.time.LocalDateTime;

/** payload: QUESTION = options·candidates, SOLUTION = steps·cautions·evidence, ESCALATE = reason. 그 외 null */
public record MessageResponse(
        Long id,
        MessageSender sender,
        MessageType type,
        String content,
        MessagePayload payload,
        LocalDateTime createdAt
) {

    public static MessageResponse from(ConsultationMessage m) {
        return new MessageResponse(m.getId(), m.getSender(), m.getType(), m.getContent(), m.getPayload(),
                m.getCreatedAt());
    }
}
