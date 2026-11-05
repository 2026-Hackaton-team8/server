package org.study.jibeo.domain.company.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.study.jibeo.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "company")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String located;

    @Column(name = "business_number", nullable = false, unique = true, length = 50)
    private String businessNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyType type;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Builder
    private Company(String name, String located, String businessNumber, CompanyType type, String phone, String email) {
        this.name = name;
        this.located = located;
        this.businessNumber = businessNumber;
        this.type = type;
        this.phone = phone;
        this.email = email;
    }
}
