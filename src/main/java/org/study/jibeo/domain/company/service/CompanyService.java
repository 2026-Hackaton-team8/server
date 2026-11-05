package org.study.jibeo.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.study.jibeo.domain.company.dto.CompanyResponse;
import org.study.jibeo.domain.company.dto.CreateCompanyRequest;
import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.domain.company.model.CompanyType;
import org.study.jibeo.domain.company.repository.CompanyRepository;
import org.study.jibeo.global.exception.ErrorCode;

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

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request) {
        String businessNumber = request.businessNumber().trim();
        if (companyRepository.existsByBusinessNumber(businessNumber)) {
            throw ErrorCode.DUPLICATE_BUSINESS_NUMBER.toException();
        }
        Company company = companyRepository.save(Company.builder()
                .name(request.name())
                .located(request.located())
                .businessNumber(businessNumber)
                .type(request.type())
                .phone(request.phone())
                .email(request.email() == null || request.email().isBlank() ? null : request.email())
                .build());
        return CompanyResponse.from(company);
    }
}