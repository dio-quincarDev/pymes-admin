package auth.pymes.service;

import auth.pymes.common.models.dto.response.UserEntityResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface UserService {
    /**
     * Obtiene los datos del usuario autenticado.
     */
    UserEntityResponse getCurrentUser(OAuth2User principal);

    /**
     * Obtiene el usuario por email.
     */
    UserEntityResponse getUserByEmail(String email);
}
