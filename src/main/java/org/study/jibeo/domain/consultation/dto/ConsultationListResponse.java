package org.study.jibeo.domain.consultation.dto;

import org.study.jibeo.domain.consultation.model.Consultation;
import org.study.jibeo.domain.consultation.model.ConsultationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 상담 내역. counts = 탭별 개수 (ALL, IN_PROGRESS, ESCALATED, RESOLVED) */
public record ConsultationListResponse(Map<String, Long> counts, List<Item> content) {

    public record Item(Long id, ConsultationStatus status, String symptom, String aiCause, int questionCount,
                       Long equipmentId, String equipmentName, String modelCode, LocalDateTime createdAt,
                       LocalDateTime resolvedAt) {

        public static Item from(Consultation c) {
            return new Item(c.getId(), c.getStatus(), c.getSymptom(), c.getAiCause(), c.getQuestionCount(),
                    c.getEquipment().getId(), c.getEquipment().getName(),
                    c.getEquipment().getEquipmentModel().getCode(), c.getCreatedAt(), c.getResolvedAt());
        }
    }
}
