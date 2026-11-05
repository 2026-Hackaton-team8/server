package org.study.jibeo.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.company.dto.CompanyResponse;
import org.study.jibeo.domain.company.model.CompanyType;
import org.study.jibeo.domain.company.repository.CompanyRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    public List<CompanyResponse> findAll(CompanyType type) {
        var companies = type == null
                ? companyRepository.findAllByOrderByNameAsc()
                : companyRepository.findAllByTypeOrderByNameAsc(type);
        return companies.stream().map(CompanyResponse::from).toList();
    }
}
