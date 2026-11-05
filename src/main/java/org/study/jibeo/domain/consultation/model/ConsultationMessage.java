package org.study.jibeo.domain.consultation.model;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.study.jibeo.global.entity.BaseEntity;

/** 상담 대화 한 줄. content = 화면에 보이는 글 (질문, 답변, 원인 문장) */
@Getter
@Entity
@Table(name = "consultation_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultationMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageSender sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    private MessagePayload payload;

    @Builder
    private ConsultationMessage(Consultation consultation, MessageSender sender, MessageType type, String content,
                                MessagePayload payload) {
        this.consultation = consultation;
        this.sender = sender;
        this.type = type;
        this.content = content;
        this.payload = payload;
    }
}
