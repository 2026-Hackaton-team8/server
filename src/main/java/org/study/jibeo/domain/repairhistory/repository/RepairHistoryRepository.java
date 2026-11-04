package org.study.jibeo.domain.repairhistory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.study.jibeo.domain.repairhistory.model.RepairHistory;

public interface RepairHistoryRepository extends JpaRepository<RepairHistory, Long> {
}
