package org.study.jibeo.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI에 JWT Bearer 인증(Authorize 버튼)을 추가한다.
 */
@Configuration
public class SwaggerAuthConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public GlobalOpenApiCustomizer bearerAuthCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSecuritySchemes(SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"));
            openApi.addSecurityItem(new SecurityRequirement().addList(SCHEME));
        };
    }
}