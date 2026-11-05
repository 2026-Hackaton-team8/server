package org.study.jibeo.global.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 자격 증명은 기본 체인을 쓴다. 서버에서는 EC2 인스턴스 역할로 동작하므로 AWS 키를 두지 않는다.
 */
@Configuration
public class S3Config {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(StorageProperties properties) {
        return S3Client.builder().region(Region.of(properties.region())).build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(StorageProperties properties) {
        return S3Presigner.builder().region(Region.of(properties.region())).build();
    }
}
