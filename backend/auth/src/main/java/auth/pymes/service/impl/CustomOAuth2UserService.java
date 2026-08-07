package auth.pymes.service.impl;

import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.enums.AuthProvider;
import auth.pymes.repositories.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserEntityRepository userEntityRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider authProvider = AuthProvider.valueOf(registrationId.toUpperCase());

        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Extraer datos comunes de OAuth2
        String providerId = getProviderId(attributes, registrationId);
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = getPictureUrl(attributes, registrationId);

        // 1. Buscar por provider + providerId (login OAuth2 normal)
        UserEntity user = userEntityRepository.findByProviderAndProviderId(authProvider, providerId)
                .orElse(null);

        if (user == null) {
            // 2. No encontrado por provider → buscar por email (account linking)
            user = userEntityRepository.findByEmail(email).orElse(null);

            if (user != null) {
                // 3. Usuario existe con otro provider → account linking
                user.setProvider(authProvider);
                user.setProviderId(providerId);
                user.setPictureUrl(picture);
                user.setName(name);
            } else {
                // 4. Usuario nuevo → crear
                user = UserEntity.builder()
                        .email(email)
                        .name(name)
                        .provider(authProvider)
                        .providerId(providerId)
                        .pictureUrl(picture)
                        .isActive(true)
                        .build();
            }

            userEntityRepository.save(user);
        }

        return oAuth2User;
    }

    private String getProviderId(Map<String, Object> attributes, String registrationId) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return (String) attributes.get("sub");
        }
        return (String) attributes.get("id");
    }

    private String getPictureUrl(Map<String, Object> attributes, String registrationId) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return (String) attributes.get("picture");
        }
        if ("facebook".equalsIgnoreCase(registrationId)) {
            Map<String, Object> picture = (Map<String, Object>) attributes.get("picture");
            if (picture != null) {
                Map<String, Object> data = (Map<String, Object>) picture.get("data");
                if (data != null) {
                    return (String) data.get("url");
                }
            }
        }
        return null;
    }
}