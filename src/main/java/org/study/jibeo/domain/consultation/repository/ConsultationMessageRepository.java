package org.study.jibeo.domain.consultation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.study.jibeo.domain.consultation.model.ConsultationMessage;

import java.util.List;

public interface ConsultationMessageRepository extends JpaRepository<ConsultationMessage, Long> {

    List<ConsultationMessage> findAllByConsultationIdOrderByIdAsc(Long consultationId);
}
