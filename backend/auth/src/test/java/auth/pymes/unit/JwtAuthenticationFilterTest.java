package auth.pymes.unit;

import auth.pymes.common.config.JwtAuthenticationFilter;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.JwtService;
import auth.pymes.utils.exception.token.TokenInvalidException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserEntityRepository userRepository;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest protectedRequest(String authorizationHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        return request;
    }

    private UserEntity activeUser(UUID id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("owner@pymes.dev");
        user.setIsActive(true);
        return user;
    }

    @Test
    @DisplayName("Ruta en WHITE_LIST: no valida token ni consulta la DB")
    void whitelistPath_NoValidaTokenNiConsultaDb() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader("Authorization", "Bearer token-que-no-debe-leerse");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    @DisplayName("Sin header Authorization: pasa sin autenticar")
    void sinHeader_PasaSinAutenticar() throws Exception {
        MockHttpServletRequest request = protectedRequest(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    @DisplayName("Header que no es Bearer: pasa sin autenticar")
    void headerNoBearer_PasaSinAutenticar() throws Exception {
        MockHttpServletRequest request = protectedRequest("Basic dXN1YXJpbzpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    @DisplayName("Token válido: autentica con ROLE_<rol> e inyecta X-Tenant-Id")
    void tokenValido_AutenticaEInyectaTenant() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserEntity user = activeUser(userId);
        when(jwtService.validateToken("tok-ok"))
                .thenReturn(new JwtService.ValidatedToken(userId, tenantId, "OWNER", user.getEmail()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = protectedRequest("Bearer tok-ok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(user);
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_OWNER");
        assertThat(request.getAttribute("X-Tenant-Id")).isEqualTo(tenantId);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Token válido pero usuario inexistente: no autentica")
    void tokenValidoUsuarioInexistente_NoAutentica() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.validateToken("tok-orfan"))
                .thenReturn(new JwtService.ValidatedToken(userId, UUID.randomUUID(), "VIEWER", "x@y.z"));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        MockHttpServletRequest request = protectedRequest("Bearer tok-orfan");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Token válido pero usuario deshabilitado: no autentica")
    void tokenValidoUsuarioDeshabilitado_NoAutentica() throws Exception {
        UUID userId = UUID.randomUUID();
        UserEntity user = activeUser(userId);
        user.setIsActive(false);
        when(jwtService.validateToken("tok-inactivo"))
                .thenReturn(new JwtService.ValidatedToken(userId, UUID.randomUUID(), "OWNER", user.getEmail()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = protectedRequest("Bearer tok-inactivo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Token inválido: 401 JSON AUTH004 y no continúa la cadena")
    void tokenInvalido_Respuesta401JsonSinInvocarCadena() throws Exception {
        when(jwtService.validateToken("tok-malo")).thenThrow(new TokenInvalidException());

        MockHttpServletRequest request = protectedRequest("Bearer tok-malo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("AUTH004");
        assertThat(chain.getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
