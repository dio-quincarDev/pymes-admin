package auth.pymes.unit;

import auth.pymes.common.config.OAuth2AuthenticationSuccessHandler;
import auth.pymes.common.models.dto.request.OAuth2IntentRequest;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.PlanName;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.JwtService;
import auth.pymes.service.OAuth2IntentService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserEntityRepository userRepository;

    @Mock
    private UserTenantRepository userTenantRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private OAuth2IntentService oauth2IntentService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    @Captor
    private ArgumentCaptor<String> redirectUriCaptor;

    private UserEntity mockUser;

    @BeforeEach
    void setUp() {
        mockUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("test@gmail.com")
                .name("Test User")
                .build();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("access-token");
        lenient().when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
    }

    @Test
    void conIntentId_EnCookie_ProcesaIntentYCreaTenant() throws Exception {
        when(authentication.getPrincipal()).thenReturn(createOAuth2User("test@gmail.com"));
        when(request.getCookies()).thenReturn(new Cookie[]{createCookie("oauth2_intent", "intent-123")});
        
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(oauth2IntentService.getIntent("intent-123"))
                .thenReturn(Optional.of(new OAuth2IntentRequest("Test Corp", "test-corp")));
        when(tenantRepository.findBySlug("test-corp")).thenReturn(Optional.empty());
        
        Tenant savedTenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Test Corp")
                .slug("test-corp")
                .plan(PlanName.FREE)
                .isActive(true)
                .build();
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(userTenantRepository.save(any(UserTenant.class))).thenReturn(null);
        
        handler.onAuthenticationSuccess(request, response, authentication);
        
        verify(oauth2IntentService).getIntent("intent-123");
        verify(oauth2IntentService).deleteIntent("intent-123");
        verify(tenantRepository).save(argThat(t -> 
                t.getName().equals("Test Corp") && t.getSlug().equals("test-corp")));
    }

    @Test
    @DisplayName("Intent con slug duplicado → DuplicateResourceException (409)")
    void conIntentId_SlugDuplicado_LanzaDuplicateResourceException() throws Exception {
        when(authentication.getPrincipal()).thenReturn(createOAuth2User("test@gmail.com"));
        when(request.getCookies()).thenReturn(new Cookie[]{createCookie("oauth2_intent", "intent-123")});
        
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(oauth2IntentService.getIntent("intent-123"))
                .thenReturn(Optional.of(new OAuth2IntentRequest("Test Corp", "test-corp")));
        
        Tenant existingTenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Test Corp")
                .slug("test-corp")
                .build();
        when(tenantRepository.findBySlug("test-corp")).thenReturn(Optional.of(existingTenant));
        
        org.junit.jupiter.api.Assertions.assertThrows(
                auth.pymes.utils.exception.custom.DuplicateResourceException.class,
                () -> handler.onAuthenticationSuccess(request, response, authentication));
        
        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(userTenantRepository, never()).save(any(UserTenant.class));
    }

    @Test
    void sinCookieYSinTenants_NoCreaWorkspace() throws Exception {
        when(authentication.getPrincipal()).thenReturn(createOAuth2User("test@gmail.com"));
        when(request.getCookies()).thenReturn(null);

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(userTenantRepository.findByUserId(mockUser.getId())).thenReturn(List.of());

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(oauth2IntentService, never()).getIntent(any());
        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(userTenantRepository, never()).save(any(UserTenant.class));
    }

    @Test
    void conIntentIdValido_GeneraTokensJWT() throws Exception {
        OAuth2User oAuth2User = createOAuth2User("test@gmail.com");
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(request.getCookies()).thenReturn(new Cookie[]{createCookie("oauth2_intent", "intent-123")});

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(oauth2IntentService.getIntent("intent-123"))
                .thenReturn(Optional.of(new OAuth2IntentRequest("Test Corp", "test-corp")));
        when(tenantRepository.findBySlug("test-corp")).thenReturn(Optional.empty());

        Tenant savedTenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Test Corp")
                .slug("test-corp")
                .plan(PlanName.FREE)
                .isActive(true)
                .build();
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(userTenantRepository.save(any(UserTenant.class))).thenReturn(null);
        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(jwtService).generateAccessToken(any(), any(), eq("OWNER"), eq("FREE"));
        verify(jwtService).generateRefreshToken(any());
    }

    @Test
    @DisplayName("Usuario con Tenant existente e IntentId → Crea NUEVA empresa del Intent")
    void conUsuarioExistenteConTenantEIntent_CreaNuevaEmpresaDeIntent() throws Exception {
        when(authentication.getPrincipal()).thenReturn(createOAuth2User("test@gmail.com"));
        when(request.getCookies()).thenReturn(new Cookie[]{createCookie("oauth2_intent", "intent-123")});
        
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        
        // El intent debe ser procesado
        when(oauth2IntentService.getIntent("intent-123"))
                .thenReturn(Optional.of(new OAuth2IntentRequest("Nueva Corp", "nueva-corp")));
        when(tenantRepository.findBySlug("nueva-corp")).thenReturn(Optional.empty());
        
        Tenant newTenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Nueva Corp")
                .slug("nueva-corp")
                .plan(PlanName.FREE)
                .build();
        when(tenantRepository.save(any(Tenant.class))).thenReturn(newTenant);
        
        handler.onAuthenticationSuccess(request, response, authentication);
        
        // Verificaciones: Se debe haber guardado el nuevo tenant
        verify(tenantRepository).save(argThat(t -> t.getName().equals("Nueva Corp")));
        verify(userTenantRepository).save(any(UserTenant.class));
        verify(oauth2IntentService).deleteIntent("intent-123");
    }

    private OAuth2User createOAuth2User(String email) {
        return new DefaultOAuth2User(
                List.of(),
                Map.of("email", email, "name", "Test User"),
                "email"
        );
    }

    private Cookie createCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        return cookie;
    }
}