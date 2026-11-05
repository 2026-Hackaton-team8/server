package org.study.jibeo.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.domain.company.model.CompanyType;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByBusinessNumber(String businessNumber);

    List<Company> findAllByOrderByNameAsc();

    List<Company> findAllByTypeOrderByNameAsc(CompanyType type);
}
