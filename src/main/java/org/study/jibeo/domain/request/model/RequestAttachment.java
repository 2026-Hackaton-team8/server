package org.study.jibeo.domain.request.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.study.jibeo.global.entity.BaseEntity;

/** 호출 신고서 첨부 파일 (S3 키). 파일은 POST /api/files 로 먼저 올린다 */
@Getter
@Entity
@Table(name = "request_attachment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @Column(name = "file_key", nullable = false, length = 255)
    private String fileKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Builder
    private RequestAttachment(Request request, String fileKey, String contentType, long size) {
        this.request = request;
        this.fileKey = fileKey;
        this.contentType = contentType;
        this.size = size;
    }
}
