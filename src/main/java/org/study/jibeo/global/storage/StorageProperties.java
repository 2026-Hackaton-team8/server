package org.study.jibeo.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** urlTtl = 파일 보기 주소의 최소 유효 시간 (만료 시각은 5분 단위로 올려 맞춘다) */
@ConfigurationProperties("storage.s3")
public record StorageProperties(String bucket, String region, Duration urlTtl) {
}