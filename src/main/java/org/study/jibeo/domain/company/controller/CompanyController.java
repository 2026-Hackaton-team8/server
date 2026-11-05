package org.study.jibeo.domain.company.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.study.jibeo.domain.company.dto.CompanyResponse;
import org.study.jibeo.domain.company.dto.CreateCompanyRequest;
import org.study.jibeo.domain.company.model.CompanyType;
import org.study.jibeo.domain.company.service.CompanyService;

import java.util.List;

/** 회원가입 전에 쓰므로 로그인 없이 공개 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    /** 회원가입 화면의 회사 선택 목록 */
    @GetMapping
    public List<CompanyResponse> findAll(@RequestParam(required = false) CompanyType type) {
        return companyService.findAll(type);
    }

    /** 소속 회사 등록. 등록 후 받은 id 로 회원가입한다 */
    @PostMapping
    public CompanyResponse create(@Valid @RequestBody CreateCompanyRequest request) {
        return companyService.create(request);
    }
}