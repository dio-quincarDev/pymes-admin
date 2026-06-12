package auth.pymes.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2IntentCookieFilter extends OncePerRequestFilter {

    private static final String INTENT_PARAM = "intentId";
    private static final String COOKIE_NAME = "oauth2_intent";
    private static final int COOKIE_MAX_AGE = 600;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        if (requestUri.startsWith("/oauth2/authorization/")) {
            String intentId = request.getParameter(INTENT_PARAM);

            if (StringUtils.hasText(intentId)) {
                Cookie cookie = new Cookie(COOKIE_NAME, intentId);
                cookie.setMaxAge(COOKIE_MAX_AGE);
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setAttribute("SameSite", "Lax");
                response.addCookie(cookie);

                log.debug("OAuth2 intent cookie establecida: intentId={}", intentId);
            }
        }

        filterChain.doFilter(request, response);
    }
}