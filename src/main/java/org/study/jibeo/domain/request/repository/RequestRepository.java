package org.study.jibeo.domain.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.study.jibeo.domain.request.model.Request;

public interface RequestRepository extends JpaRepository<Request, Long> {
}
