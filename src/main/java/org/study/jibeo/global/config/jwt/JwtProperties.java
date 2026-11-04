package org.study.jibeo.global.config.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenValidityMs,
        long refreshTokenValidityMs
) {
}
