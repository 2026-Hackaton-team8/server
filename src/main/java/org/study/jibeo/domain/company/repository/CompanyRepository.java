package org.study.jibeo.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.study.jibeo.domain.company.model.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
