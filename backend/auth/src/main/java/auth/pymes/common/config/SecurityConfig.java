package auth.pymes.common.config;
import auth.pymes.common.constants.ApiPathConstants;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.impl.CustomOAuth2UserService;
import auth.pymes.utils.exception.CodigoError;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserEntityRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final String[] WHITE_LIST = {
            // Swagger / OpenAPI
            "/v3/api-docs/**",
            "/swagger-ui/**",
            // Actuator
            "/actuator/**",
            // OAuth2 login endpoint
            "/oauth2/**",
            "/login/**",
            // Error page
            "/error",
            // Public auth endpoints (registration, login, email verification)
            ApiPathConstants.FULL_AUTH_REGISTER,
            ApiPathConstants.FULL_AUTH_LOGIN,
            ApiPathConstants.FULL_AUTH_REFRESH,
            ApiPathConstants.FULL_AUTH_VERIFY_EMAIL,
            ApiPathConstants.FULL_AUTH_RESEND_VERIFICATION,
            // Password recovery (public by design)
            ApiPathConstants.FULL_AUTH_FORGOT_PASSWORD,
            ApiPathConstants.FULL_AUTH_RESET_PASSWORD,
            ApiPathConstants.FULL_AUTH_OAUTH2_INTENT
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                );

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "codigo", CodigoError.UNAUTHORIZED_ACCESS.getCodigo(),
                    "mensaje", authException.getMessage() != null ? authException.getMessage() : CodigoError.UNAUTHORIZED_ACCESS.getMensaje(),
                    "path", request.getRequestURI(),
                    "timestamp", Instant.now().toString(),
                    "detalles", Map.of()
            )));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response, org.springframework.security.access.AccessDeniedException accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "codigo", CodigoError.INSUFFICIENT_PERMISSIONS.getCodigo(),
                    "mensaje", CodigoError.INSUFFICIENT_PERMISSIONS.getMensaje(),
                    "path", request.getRequestURI(),
                    "timestamp", Instant.now().toString(),
                    "detalles", Map.of()
            )));
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }
}