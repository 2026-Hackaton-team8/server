package org.study.jibeo.global.storage;

import org.junit.jupiter.api.Test;
import org.study.jibeo.global.config.jwt.JwtProperties;
import team.themoment.sdk.exception.ExpectedException;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileViewUrlsTest {

    private static final String KEY = "uploads/requests/1/2026/11/a.jpg";

    private final FileViewUrls urls = new FileViewUrls(
            new JwtProperties("test-secret-key-for-unit-tests-must-be-at-least-32-bytes-long", 1000, 1000),
            new StorageProperties("bucket", "ap-northeast-3", Duration.ofMinutes(10)));

    @Test
    void 만료_시각은_최소_10분_뒤를_5분_단위로_올려_맞춘다() {
        assertThat(urls.expiresAt(1_000)).isEqualTo(1_800);   // 1600 → 1800
        assertThat(urls.expiresAt(1_200)).isEqualTo(1_800);   // 1800 그대로
        assertThat(urls.expiresAt(1_201)).isEqualTo(2_100);
    }

    @Test
    void 유효한_서명이면_남은_시간을_준다() {
        long exp = Instant.now().getEpochSecond() + 600;

        assertThat(urls.verify(KEY, exp, urls.sign(KEY, exp))).isBetween(590L, 600L);
    }

    @Test
    void 만료됐거나_업로드_경로가_아니면_거절한다() {
        long past = Instant.now().getEpochSecond() - 1;
        long exp = Instant.now().getEpochSecond() + 600;
        String outside = "private/secret.txt";
        String traversal = "uploads/../private/secret.txt";

        assertThatThrownBy(() -> urls.verify(KEY, past, urls.sign(KEY, past))).isInstanceOf(ExpectedException.class);
        assertThatThrownBy(() -> urls.verify(outside, exp, urls.sign(outside, exp))).isInstanceOf(ExpectedException.class);
        assertThatThrownBy(() -> urls.verify(traversal, exp, urls.sign(traversal, exp))).isInstanceOf(ExpectedException.class);
    }
}
