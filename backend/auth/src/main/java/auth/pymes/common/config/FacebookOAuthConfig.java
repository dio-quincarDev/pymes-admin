package auth.pymes.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnExpression("T(java.lang.System).getenv('FACEBOOK_CLIENT_ID') != null and !T(java.lang.System).getenv('FACEBOOK_CLIENT_ID').isEmpty()")
public class FacebookOAuthConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        // Google (from env vars)
        registrations.add(ClientRegistration.withRegistrationId("google")
                .clientId(System.getenv("GOOGLE_CLIENT_ID"))
                .clientSecret(System.getenv("GOOGLE_CLIENT_SECRET"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .scope("email", "profile")
                .redirectUri((System.getenv("OAUTH2_REDIRECT_URI") != null ? System.getenv("OAUTH2_REDIRECT_URI") : "http://localhost:8080") + "/login/oauth2/code/google")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build());

        // Facebook (conditional)
        registrations.add(ClientRegistration.withRegistrationId("facebook")
                .clientId(System.getenv("FACEBOOK_CLIENT_ID"))
                .clientSecret(System.getenv("FACEBOOK_CLIENT_SECRET"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .scope("email", "public_profile")
                .redirectUri((System.getenv("OAUTH2_REDIRECT_URI") != null ? System.getenv("OAUTH2_REDIRECT_URI") : "http://localhost:8080") + "/login/oauth2/code/facebook")
                .authorizationUri("https://www.facebook.com/v18.0/dialog/oauth")
                .tokenUri("https://graph.facebook.com/v18.0/oauth/access_token")
                .userInfoUri("https://graph.facebook.com/v18.0/me?fields=id,name,email")
                .userNameAttributeName("id")
                .clientName("Facebook")
                .build());

        return new InMemoryClientRegistrationRepository(registrations);
    }
}
