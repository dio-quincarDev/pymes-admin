package dev.dioquincar.gateway_pymes.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Aggregates Swagger UI URLs from all microservices into the Gateway's Swagger UI dropdown.
 * This allows accessing all service documentation from a single entry point.
 *
 * Usage: Access Swagger UI at http://localhost:8080/swagger-ui.html
 * The dropdown will list all registered microservices.
 */
@Component
@RequiredArgsConstructor
public class SwaggerAggregatorConfig {

    private final SwaggerUiConfigProperties swaggerUiConfig;

    @PostConstruct
    public void init() {
        swaggerUiConfig.setUrls(Set.of(
                new SwaggerUrl("auth", "/v3/api-docs/auth", "Auth Service")
        ));
    }
}
