package org.study.jibeo.domain.equipment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.study.jibeo.domain.equipment.model.Equipment;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    @Query("""
            SELECT e FROM Equipment e JOIN FETCH e.equipmentModel
            WHERE e.company.id = :companyId
            ORDER BY e.id DESC
            """)
    List<Equipment> findAllWithModelByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            SELECT e FROM Equipment e JOIN FETCH e.equipmentModel m JOIN FETCH m.makerCompany
            WHERE e.id = :id
            """)
    Optional<Equipment> findWithModelById(@Param("id") Long id);

    /** 제작 업체의 모델별 설치 대수 */
    @Query("""
            SELECT e.equipmentModel.id AS modelId, COUNT(e) AS count
            FROM Equipment e
            WHERE e.equipmentModel.makerCompany.id = :makerCompanyId
            GROUP BY e.equipmentModel.id
            """)
    List<ModelCount> countByModelForMaker(@Param("makerCompanyId") Long makerCompanyId);

    interface ModelCount {
        Long getModelId();

        long getCount();
    }
}
