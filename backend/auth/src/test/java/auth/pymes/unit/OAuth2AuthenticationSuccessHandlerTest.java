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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    }

    @Test
    void conIntentId_EnCookie_ProcesaIntentYCreaTenant() throws Exception {
        when(authentication.getPrincipal()).thenReturn(createOAuth2User("test@gmail.com"));
        when(request.getCookies()).thenReturn(new Cookie[]{createCookie("oauth2_intent", "intent-123")});
        
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(oauth2IntentService.getIntent("intent-123"))
                .thenReturn(Optional.of(new OAuth2IntentRequest("Test Corp", "test-corp")));
        
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
    void sinCookie_CreaTenantDefault() throws Exception {
        when(authentication.getPrincipal()).thenReturn(createOAuth2User("test@gmail.com"));
        when(request.getCookies()).thenReturn(null);
        
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(userTenantRepository.findByUserId(mockUser.getId())).thenReturn(List.of());
        
        Tenant savedTenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Mi Empresa")
                .build();
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(userTenantRepository.save(any(UserTenant.class))).thenReturn(null);
        
        handler.onAuthenticationSuccess(request, response, authentication);
        
        verify(oauth2IntentService, never()).getIntent(any());
        verify(tenantRepository).save(argThat(t -> t.getName().equals("Mi Empresa")));
    }

    @Test
    void conIntentIdValido_GeneraTokensJWT() throws Exception {
        OAuth2User oAuth2User = createOAuth2User("test@gmail.com");
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(request.getCookies()).thenReturn(new Cookie[]{createCookie("oauth2_intent", "intent-123")});

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(oauth2IntentService.getIntent("intent-123"))
                .thenReturn(Optional.of(new OAuth2IntentRequest("Test Corp", "test-corp")));

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