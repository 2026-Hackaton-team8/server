package org.study.jibeo.domain.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.study.jibeo.global.entity.BaseEntity;
import org.study.jibeo.domain.company.model.Company;
import org.study.jibeo.global.exception.ErrorCode;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false, length = 100)
    private String pw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    /** 역할 미정(PENDING)일 때는 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Builder
    private User(String loginId, String pw, UserRole role, Company company, String name, String phone, String email) {
        this.loginId = loginId;
        this.pw = pw;
        this.role = role;
        this.company = company;
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    /** 역할 미정 상태에서 한 번만 역할과 소속 회사를 정한다 */
    public void selectRole(UserRole role, Company company) {
        if (this.role != UserRole.PENDING) {
            throw ErrorCode.ROLE_ALREADY_SELECTED.toException();
        }
        this.role = role;
        this.company = company;
    }

    public Long getCompanyId() {
        return company == null ? null : company.getId();
    }

    public void updateContact(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
    }
}
