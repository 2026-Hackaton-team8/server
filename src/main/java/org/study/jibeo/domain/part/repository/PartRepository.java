package org.study.jibeo.domain.part.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.study.jibeo.domain.part.model.Part;

import java.util.List;

public interface PartRepository extends JpaRepository<Part, Long> {

    List<Part> findAllByEquipmentModelIdOrderByCodeAsc(Long equipmentModelId);

    boolean existsByEquipmentModelIdAndCode(Long equipmentModelId, String code);

    long countByEquipmentModelId(Long equipmentModelId);

    java.util.Optional<Part> findByEquipmentModelIdAndCode(Long equipmentModelId, String code);
}
