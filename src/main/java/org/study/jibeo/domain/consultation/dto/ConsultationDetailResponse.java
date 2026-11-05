package org.study.jibeo.domain.consultation.dto;

import org.study.jibeo.domain.consultation.model.Consultation;
import org.study.jibeo.domain.consultation.model.ConsultationStatus;
import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.equipmentmodel.dto.PartResponse;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상담 상세. canAnswer = AI 질문에 답할 차례, canResolve = "해결됐어요"·담당자 호출을 할 수 있음.
 * suspectedPart.meshName 으로 3D 뷰어에서 부품을 강조한다.
 */
public record ConsultationDetailResponse(
        Long id,
        ConsultationStatus status,
        String symptom,
        int questionCount,
        int maxQuestions,
        String aiCause,
        PartResponse suspectedPart,
        EquipmentInfo equipment,
        Long requestId,
        boolean canAnswer,
        boolean canResolve,
        List<MessageResponse> messages,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {

    public record EquipmentInfo(Long id, String name, String location, Long modelId, String modelCode,
                                String modelName) {
    }

    public static ConsultationDetailResponse of(Consultation c, List<MessageResponse> messages, boolean canAnswer,
                                                Long requestId) {
        Equipment e = c.getEquipment();
        EquipmentModel m = e.getEquipmentModel();
        boolean open = c.getStatus() == ConsultationStatus.IN_PROGRESS;
        return new ConsultationDetailResponse(c.getId(), c.getStatus(), c.getSymptom(), c.getQuestionCount(),
                Consultation.MAX_QUESTIONS, c.getAiCause(),
                c.getSuspectedPart() == null ? null : PartResponse.from(c.getSuspectedPart()),
                new EquipmentInfo(e.getId(), e.getName(), e.getLocation(), m.getId(), m.getCode(), m.getName()),
                requestId, open && canAnswer, open, messages, c.getCreatedAt(), c.getResolvedAt());
    }
}
