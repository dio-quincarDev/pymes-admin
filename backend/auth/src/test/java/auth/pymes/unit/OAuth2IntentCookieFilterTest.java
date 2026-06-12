package auth.pymes.unit;

import auth.pymes.common.config.OAuth2IntentCookieFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2IntentCookieFilterTest {

    private OAuth2IntentCookieFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new OAuth2IntentCookieFilter();
    }

    private void invokeDoFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws Exception {
        filter.doFilter(request, response, chain);
    }

    @Test
    void conIntentId_CreaCookieConValorCorrecto() throws Exception {
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/google");
        when(request.getParameter("intentId")).thenReturn("test-intent-123");

        invokeDoFilter(request, response, filterChain);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        assertThat(cookieCaptor.getValue().getName()).isEqualTo("oauth2_intent");
        assertThat(cookieCaptor.getValue().getValue()).isEqualTo("test-intent-123");
    }

    @Test
    void sinIntentId_NoCreaCookie() throws Exception {
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/google");
        when(request.getParameter("intentId")).thenReturn(null);

        invokeDoFilter(request, response, filterChain);

        verify(response, never()).addCookie(any(Cookie.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void pathNoOAuth2_NoHaceNada() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/users");

        invokeDoFilter(request, response, filterChain);

        verify(response, never()).addCookie(any(Cookie.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void intentIdVacio_SinCookie() throws Exception {
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/google");
        when(request.getParameter("intentId")).thenReturn("");

        invokeDoFilter(request, response, filterChain);

        verify(response, never()).addCookie(any(Cookie.class));
    }

    @Test
    void intentIdSoloEspacios_SinCookie() throws Exception {
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/google");
        when(request.getParameter("intentId")).thenReturn("   ");

        invokeDoFilter(request, response, filterChain);

        verify(response, never()).addCookie(any(Cookie.class));
    }

    @Test
    void cookieMaxAgeYPathCorrectos() throws Exception {
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/google");
        when(request.getParameter("intentId")).thenReturn("test-intent-123");

        invokeDoFilter(request, response, filterChain);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();
        assertThat(cookie.getMaxAge()).isEqualTo(600);
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void continuaFilterChain() throws Exception {
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/google");
        when(request.getParameter("intentId")).thenReturn("test-intent-123");

        invokeDoFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}