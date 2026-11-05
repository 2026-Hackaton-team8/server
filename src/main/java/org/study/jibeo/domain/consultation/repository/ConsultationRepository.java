package org.study.jibeo.domain.consultation.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.study.jibeo.domain.consultation.model.Consultation;
import org.study.jibeo.domain.consultation.model.ConsultationStatus;

import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    @Query("""
            SELECT c FROM Consultation c
            JOIN FETCH c.equipment e JOIN FETCH e.equipmentModel
            JOIN FETCH c.company
            LEFT JOIN FETCH c.suspectedPart
            WHERE c.id = :id
            """)
    Optional<Consultation> findDetailById(@Param("id") Long id);

    /** 상담 내역 (status, equipmentId 가 null 이면 조건 없음) */
    @EntityGraph(attributePaths = {"equipment", "equipment.equipmentModel"})
    @Query("""
            SELECT c FROM Consultation c
            WHERE c.company.id = :companyId
              AND (:status IS NULL OR c.status = :status)
              AND (:equipmentId IS NULL OR c.equipment.id = :equipmentId)
            ORDER BY c.id DESC
            """)
    List<Consultation> findList(@Param("companyId") Long companyId, @Param("status") ConsultationStatus status,
                                @Param("equipmentId") Long equipmentId);

    @Query("""
            SELECT c.status AS status, COUNT(c) AS count FROM Consultation c
            WHERE c.company.id = :companyId AND (:equipmentId IS NULL OR c.equipment.id = :equipmentId)
            GROUP BY c.status
            """)
    List<StatusCount> countByStatus(@Param("companyId") Long companyId, @Param("equipmentId") Long equipmentId);

    interface StatusCount {
        ConsultationStatus getStatus();

        long getCount();
    }
}
