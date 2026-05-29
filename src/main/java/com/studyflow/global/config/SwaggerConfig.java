package com.studyflow.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    // @AuthenticationPrincipal Long userId 파라미터를 Swagger 문서에서 제거
    // SpringDoc이 해당 파라미터를 처리하지 못해 500 에러가 발생하는 것을 방지
    @Bean
    public OperationCustomizer removeAuthPrincipalParam() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() != null) {
                operation.getParameters().removeIf(p -> "userId".equals(p.getName()));
            }
            return operation;
        };
    }

    @Bean
    public OpenAPI openAPI() {
        // Authorize 버튼 클릭 후 토큰 입력 시 모든 API 요청에 Bearer 헤더 자동 추가
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("StudyFlow API")
                        .description("StudyFlow 백엔드 API 문서")
                        .version("v1.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerAuth))
                .addSecurityItem(securityRequirement);
    }
}
