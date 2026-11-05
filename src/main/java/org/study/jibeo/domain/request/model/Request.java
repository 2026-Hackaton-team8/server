package org.study.jibeo.domain.request.model;

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
import org.study.jibeo.domain.equipment.model.Equipment;
import org.study.jibeo.domain.user.model.User;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "request")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Request extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder
    private Request(Equipment equipment, LocalDateTime requestedAt, Company company, User user, RequestType type, String description) {
        this.equipment = equipment;
        this.requestedAt = requestedAt;
        this.company = company;
        this.user = user;
        this.type = type;
        this.description = description;
    }
}
