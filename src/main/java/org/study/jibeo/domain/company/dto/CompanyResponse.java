package org.study.jibeo.domain.company.dto;

import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.domain.company.model.CompanyType;

public record CompanyResponse(Long id, String name, CompanyType type, String located) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getType(), company.getLocated());
    }
}
