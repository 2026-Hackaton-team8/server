package org.study.jibeo.domain.solution.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.study.jibeo.domain.solution.model.Solution;
import org.study.jibeo.domain.solution.model.SolutionStatus;

import java.util.List;
import java.util.Optional;

public interface SolutionRepository extends JpaRepository<Solution, Long> {

    @Query("""
            SELECT s FROM Solution s
            JOIN FETCH s.equipmentModel m JOIN FETCH m.makerCompany
            LEFT JOIN FETCH s.approvedBy
            WHERE s.id = :id
            """)
    Optional<Solution> findDetailById(@Param("id") Long id);

    @Query("""
            SELECT s FROM Solution s
            WHERE s.equipmentModel.id = :modelId AND (:status IS NULL OR s.status = :status)
            ORDER BY s.id DESC
            """)
    List<Solution> findList(@Param("modelId") Long modelId, @Param("status") SolutionStatus status);

    @Query("""
            SELECT s.status AS status, COUNT(s) AS count FROM Solution s
            WHERE s.equipmentModel.id = :modelId
            GROUP BY s.status
            """)
    List<StatusCount> countByStatus(@Param("modelId") Long modelId);

    /** 제작 업체의 모델별·상태별 솔루션 수 (모델 목록 화면) */
    @Query("""
            SELECT s.equipmentModel.id AS modelId, s.status AS status, COUNT(s) AS count FROM Solution s
            WHERE s.equipmentModel.makerCompany.id = :makerCompanyId
            GROUP BY s.equipmentModel.id, s.status
            """)
    List<ModelStatusCount> countByModelForMaker(@Param("makerCompanyId") Long makerCompanyId);

    interface StatusCount {
        SolutionStatus getStatus();

        long getCount();
    }

    interface ModelStatusCount {
        Long getModelId();

        SolutionStatus getStatus();

        long getCount();
    }
}
