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

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserEntityRepository userEntityRepository;

    @Override
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

        // Buscar o crear el usuario en la base de datos
        userEntityRepository.findByProviderAndProviderId(authProvider, providerId)
                .orElseGet(() -> {
                    UserEntity newUser = UserEntity.builder()
                            .email(email)
                            .name(name)
                            .provider(authProvider)
                            .providerId(providerId)
                            .pictureUrl(picture)
                            .isActive(true)
                            .build();
                    return userEntityRepository.save(newUser);
                });

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