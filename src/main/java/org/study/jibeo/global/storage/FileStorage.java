package org.study.jibeo.global.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.study.jibeo.global.exception.ErrorCode;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import team.themoment.sdk.exception.ExpectedException;

import java.io.InputStream;
import java.util.Optional;

/** S3 버킷에 파일을 저장하고 읽는다 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorage {

    private final S3Client s3Client;
    private final StorageProperties properties;

    public record FileMeta(String contentType, long size) {
    }

    /** contentRange 는 구간 요청일 때만 있다 (예: bytes 0-1023/20480) */
    public record FileStream(InputStream body, String contentType, Long contentLength, String contentRange,
                             String eTag) {
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

    /** range 는 HTTP Range 헤더 값 (예: bytes=0-1023), 없으면 전체. 받은 쪽에서 body 를 닫아야 한다 */
    public Optional<FileStream> open(String key, String range) {
        try {
            ResponseInputStream<GetObjectResponse> in = s3Client.getObject(r -> r.bucket(properties.bucket()).key(key)
                    .range(range));
            GetObjectResponse res = in.response();
            return Optional.of(new FileStream(in, res.contentType(), res.contentLength(), res.contentRange(),
                    res.eTag()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            if (e.statusCode() == 416) {
                throw ErrorCode.RANGE_NOT_SATISFIABLE.toException();
            }
            log.error("S3 get failed: key={}", key, e);
            throw storageFailed();
        } catch (SdkException e) {
            log.error("S3 get failed: key={}", key, e);
            throw storageFailed();
        }
    }

    private static ExpectedException storageFailed() {
        return new ExpectedException("파일 저장소에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
    }
}