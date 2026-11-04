package org.study.jibeo.domain.part.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.study.jibeo.domain.part.model.Part;

public interface PartRepository extends JpaRepository<Part, Long> {
}
