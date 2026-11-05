package org.study.jibeo.domain.equipmentmodel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.study.jibeo.domain.equipmentmodel.model.ManualSection;

import java.util.List;
import java.util.Optional;

public interface ManualSectionRepository extends JpaRepository<ManualSection, Long> {

    List<ManualSection> findAllByEquipmentModelIdOrderBySortOrderAsc(Long equipmentModelId);

    Optional<ManualSection> findByEquipmentModelIdAndSectionKey(Long equipmentModelId, String sectionKey);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ManualSection s WHERE s.equipmentModel.id = :modelId")
    void deleteAllByModelId(@Param("modelId") Long modelId);
}
