package org.study.jibeo.domain.company.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.company.dto.CompanyResponse;
import org.study.jibeo.domain.company.model.CompanyType;
import org.study.jibeo.domain.company.service.CompanyService;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    /** 회원가입 화면의 회사 선택 목록 (공개) */
    @GetMapping
    public List<CompanyResponse> findAll(@RequestParam(required = false) CompanyType type) {
        return companyService.findAll(type);
    }
}
