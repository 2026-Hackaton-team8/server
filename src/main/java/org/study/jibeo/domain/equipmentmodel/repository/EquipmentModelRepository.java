package org.study.jibeo.domain.equipmentmodel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.study.jibeo.domain.equipmentmodel.model.EquipmentModel;

import java.util.List;
import java.util.Optional;

public interface EquipmentModelRepository extends JpaRepository<EquipmentModel, Long> {

    @Query("""
            SELECT m FROM EquipmentModel m JOIN FETCH m.makerCompany
            WHERE :q IS NULL
               OR LOWER(m.code) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY m.code
            """)
    List<EquipmentModel> search(@Param("q") String q);

    @Query("SELECT m FROM EquipmentModel m JOIN FETCH m.makerCompany WHERE m.id = :id")
    Optional<EquipmentModel> findWithMakerById(@Param("id") Long id);

    List<EquipmentModel> findAllByMakerCompanyIdOrderByCodeAsc(Long makerCompanyId);

    boolean existsByMakerCompanyIdAndCode(Long makerCompanyId, String code);
}
