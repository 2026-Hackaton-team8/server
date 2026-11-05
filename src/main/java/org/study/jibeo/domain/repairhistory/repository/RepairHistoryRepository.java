package org.study.jibeo.domain.repairhistory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.study.jibeo.domain.repairhistory.model.RepairHistory;

import java.time.LocalDate;
import java.util.List;

public interface RepairHistoryRepository extends JpaRepository<RepairHistory, Long> {

    long countByEquipmentEquipmentModelId(Long equipmentModelId);

    /** 제작 업체의 모델별 수리 이력 수와 최근 수리일 */
    @Query("""
            SELECT r.equipment.equipmentModel.id AS modelId, COUNT(r) AS count, MAX(r.repairedAt) AS lastRepairedAt
            FROM RepairHistory r
            WHERE r.equipment.equipmentModel.makerCompany.id = :makerCompanyId
            GROUP BY r.equipment.equipmentModel.id
            """)
    List<ModelRepairStat> statByModelForMaker(@Param("makerCompanyId") Long makerCompanyId);

    interface ModelRepairStat {
        Long getModelId();

        long getCount();

        LocalDate getLastRepairedAt();
    }
}
