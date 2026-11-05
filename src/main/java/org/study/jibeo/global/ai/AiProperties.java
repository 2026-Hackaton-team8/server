package org.study.jibeo.global.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Python AI 서비스 설정. baseUrl 이 비어 있으면 실제로 호출하지 않고 임시 응답(StubAi)을 쓴다.
 */
@ConfigurationProperties("ai")
public record AiProperties(String baseUrl, Duration consultationTimeout, Duration draftTimeout) {

    public boolean enabled() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
