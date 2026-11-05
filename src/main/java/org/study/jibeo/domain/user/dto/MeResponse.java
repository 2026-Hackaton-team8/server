package org.study.jibeo.domain.user.dto;

import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.domain.company.model.CompanyType;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.domain.user.model.UserRole;

public record MeResponse(
        Long userId,
        String loginId,
        String name,
        String phone,
        String email,
        UserRole role,
        CompanyInfo company
) {

    public record CompanyInfo(Long id, String name, CompanyType type, String phone, String email) {
    }

    public static MeResponse from(User user) {
        Company c = user.getCompany();
        return new MeResponse(user.getId(), user.getLoginId(), user.getName(), user.getPhone(), user.getEmail(),
                user.getRole(), new CompanyInfo(c.getId(), c.getName(), c.getType(), c.getPhone(), c.getEmail()));
    }
}
