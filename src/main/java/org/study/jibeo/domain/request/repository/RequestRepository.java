package org.study.jibeo.domain.request.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.study.jibeo.domain.request.model.Request;
import org.study.jibeo.domain.request.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    @Query("""
            SELECT r FROM Request r
            JOIN FETCH r.equipment e JOIN FETCH e.equipmentModel
            JOIN FETCH r.company
            LEFT JOIN FETCH r.engineer
            WHERE r.id = :id
            """)
    Optional<Request> findDetailById(@Param("id") Long id);

    /** 사용 업체: 우리 회사가 보낸 호출 */
    @EntityGraph(attributePaths = {"equipment", "equipment.equipmentModel", "company"})
    List<Request> findAllByCompanyIdOrderByIdDesc(Long companyId);

    /** 제작 업체: 호출 수신함 (status 가 null 이면 전체) */
    @EntityGraph(attributePaths = {"equipment", "equipment.equipmentModel", "company"})
    @Query("""
            SELECT r FROM Request r
            WHERE r.makerCompany.id = :makerCompanyId AND (:status IS NULL OR r.status = :status)
            ORDER BY r.id DESC
            """)
    Page<Request> findInbox(@Param("makerCompanyId") Long makerCompanyId, @Param("status") RequestStatus status,
                            Pageable pageable);

    @Query("""
            SELECT r.status AS status, COUNT(r) AS count FROM Request r
            WHERE r.makerCompany.id = :makerCompanyId
            GROUP BY r.status
            """)
    List<StatusCount> countByStatusForMaker(@Param("makerCompanyId") Long makerCompanyId);

    interface StatusCount {
        RequestStatus getStatus();

        long getCount();
    }
}
