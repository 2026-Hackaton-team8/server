package org.study.jibeo.global.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import team.themoment.sdk.exception.ExpectedException;

import java.io.InputStream;
import java.util.Optional;

/** S3 버킷에 파일을 저장하고, 보기용 presigned GET URL 을 만든다 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorage {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final StorageProperties properties;

    public record FileMeta(String contentType, long size) {
    }

    public void upload(String key, InputStream content, long size, String contentType) {
        try {
            s3Client.putObject(r -> r.bucket(properties.bucket()).key(key).contentType(contentType).contentLength(size),
                    RequestBody.fromInputStream(content, size));
        } catch (SdkException e) {
            log.error("S3 upload failed: key={}", key, e);
            throw storageFailed();
        }
    }

    public Optional<FileMeta> find(String key) {
        try {
            HeadObjectResponse head = s3Client.headObject(r -> r.bucket(properties.bucket()).key(key));
            return Optional.of(new FileMeta(head.contentType(), head.contentLength()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            log.error("S3 head failed: key={}", key, e);
            throw storageFailed();
        } catch (SdkException e) {
            log.error("S3 head failed: key={}", key, e);
            throw storageFailed();
        }
    }

    public String viewUrl(String key) {
        return presigner.presignGetObject(r -> r.signatureDuration(properties.urlTtl())
                        .getObjectRequest(g -> g.bucket(properties.bucket()).key(key)))
                .url().toString();
    }

    private static ExpectedException storageFailed() {
        return new ExpectedException("파일 저장소에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
    }
}
