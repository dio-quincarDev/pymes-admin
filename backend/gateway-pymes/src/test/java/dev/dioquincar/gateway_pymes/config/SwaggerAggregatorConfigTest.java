package dev.dioquincar.gateway_pymes.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerAggregatorConfigTest {

    @Test
    @DisplayName("G-UT1: registra únicamente la URL real de Auth en el agregador")
    void initRegistraUrlDeAuth() {
        SwaggerUiConfigProperties properties = new SwaggerUiConfigProperties();

        new SwaggerAggregatorConfig(properties).init();

        // Core no expone OpenAPI todavía: no debe registrarse una URL rota.
        assertThat(properties.getUrls())
                .containsExactly(new SwaggerUrl("auth", "/v3/api-docs/auth", "Auth Service"));
    }
}
