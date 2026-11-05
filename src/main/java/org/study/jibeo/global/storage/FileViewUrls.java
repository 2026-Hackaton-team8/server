package org.study.jibeo.global.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.study.jibeo.global.config.jwt.JwtProperties;
import org.study.jibeo.global.exception.ErrorCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * 첨부 파일 보기 주소 (GET /api/files/view?key=&exp=&sig=).
 * <p>
 * S3 presigned URL 은 인스턴스 역할 자격 증명으로 서명되는데, 계정 정책(명시적 Deny)이 그 자격 증명을 서버 밖에서 쓰는 것을 막는다.
 * 그래서 우리 서버가 S3 에서 읽어 내려주고, 주소에 만료 시각과 HMAC 서명을 붙여 JWT 없이 열 수 있게 한다 ({@code <img src>} 에 그대로 사용).
 * 누가 볼 수 있는지는 주소를 내주는 상세·업로드 API 가 이미 확인했다.
 */
@Component
public class FileViewUrls {

    public static final String PATH = "/api/files/view";

    /** 만료 시각을 이 단위로 올려 맞춘다. 같은 파일의 주소가 한동안 같아서 브라우저가 캐시할 수 있다 */
    private static final long WINDOW_SECONDS = 300;
    private static final String ALGORITHM = "HmacSHA256";
    private static final String KEY_PREFIX = "uploads/";

    private final SecretKeySpec secret;
    private final Duration ttl;

    public FileViewUrls(JwtProperties jwtProperties, StorageProperties storageProperties) {
        this.secret = new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
        this.ttl = storageProperties.urlTtl();
    }

    /** 현재 요청의 서버 주소 기준 절대 URL (프록시 뒤에서는 X-Forwarded-* 를 따른다) */
    public String urlOf(String key) {
        long exp = expiresAt(Instant.now().getEpochSecond());
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(PATH)
                .queryParam("key", key)
                .queryParam("exp", exp)
                .queryParam("sig", sign(key, exp))
                .build()
                .encode()
                .toUriString();
    }

    /** 서명·만료를 확인하고 남은 유효 시간(초)을 돌려준다 */
    public long verify(String key, long exp, String sig) {
        long remaining = exp - Instant.now().getEpochSecond();
        if (remaining <= 0 || !key.startsWith(KEY_PREFIX) || key.contains("..") || sig == null
                || !MessageDigest.isEqual(sign(key, exp).getBytes(StandardCharsets.US_ASCII),
                sig.getBytes(StandardCharsets.US_ASCII))) {
            throw ErrorCode.FILE_URL_INVALID.toException();
        }
        return remaining;
    }

    long expiresAt(long nowEpochSecond) {
        long earliest = nowEpochSecond + ttl.toSeconds();
        return (earliest + WINDOW_SECONDS - 1) / WINDOW_SECONDS * WINDOW_SECONDS;
    }

    String sign(String key, long exp) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secret);
            byte[] digest = mac.doFinal(("file-view\n" + key + "\n" + exp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}