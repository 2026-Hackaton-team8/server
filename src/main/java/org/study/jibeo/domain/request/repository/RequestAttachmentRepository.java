package org.study.jibeo.domain.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.study.jibeo.domain.request.model.RequestAttachment;

import java.util.List;

public interface RequestAttachmentRepository extends JpaRepository<RequestAttachment, Long> {

    List<RequestAttachment> findAllByRequestIdOrderByIdAsc(Long requestId);
}
