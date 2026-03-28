package auth.pymes.service.impl;

import auth.pymes.common.models.entities.UserEntity;
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

        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Extraer datos según el proveedor (Google/FB)
        String providerId = attributes.get("sub") != null ?
                attributes.get("sub").toString() : attributes.get("id").toString();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get(provider.equals("google") ? "picture" : "url");

        // Buscar o Crear el usuario en tu tabla 'users'
        UserEntity user = userEntityRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setProvider(provider);
                    newUser.setProviderId(providerId);
                    newUser.setPictureUrl(picture);
                    newUser.setIsActive(true);
                    return userEntityRepository.save(newUser);
                });

        return oAuth2User; // Spring Security se encarga del rest

    }
}