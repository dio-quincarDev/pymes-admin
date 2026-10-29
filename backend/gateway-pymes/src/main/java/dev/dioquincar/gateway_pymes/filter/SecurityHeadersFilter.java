package dev.dioquincar.gateway_pymes.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        // Dev: permite conexion al gateway (localhost:8080); Produccion: connect-src 'self' como base
        String csp = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data:; font-src 'self'; connect-src 'self' http://localhost:8080; " +
                "frame-ancestors 'none'; form-action 'self'; base-uri 'self'";
        headers.set("Content-Security-Policy", csp);
        headers.set("Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=()");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
        headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
