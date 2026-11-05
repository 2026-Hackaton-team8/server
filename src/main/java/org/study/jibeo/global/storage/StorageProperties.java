package org.study.jibeo.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** urlTtl = 파일 보기용 presigned GET URL 유효 시간 */
@ConfigurationProperties("storage.s3")
public record StorageProperties(String bucket, String region, Duration urlTtl) {
}
