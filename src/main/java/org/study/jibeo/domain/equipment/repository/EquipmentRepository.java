package org.study.jibeo.domain.equipment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.study.jibeo.domain.equipment.model.Equipment;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
}
