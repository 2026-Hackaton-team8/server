package org.study.jibeo.global.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 자격 증명은 기본 체인을 쓴다. 서버에서는 EC2 인스턴스 역할로 동작하므로 AWS 키를 두지 않는다.
 * 그 자격 증명은 서버 밖에서 쓸 수 없어서 presigned URL 대신 {@link FileViewUrls} 로 서버가 대신 내려준다.
 */
@Configuration
public class S3Config {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(StorageProperties properties) {
        return S3Client.builder().region(Region.of(properties.region())).build();
    }
}