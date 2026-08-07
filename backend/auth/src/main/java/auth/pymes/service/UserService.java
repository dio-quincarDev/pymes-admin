package auth.pymes.service;

import auth.pymes.common.models.dto.response.UserEntityResponse;

public interface UserService {
    /**
     * Obtiene los datos del usuario autenticado.
     */
    UserEntityResponse getCurrentUser(Object principal);

    /**
     * Obtiene el usuario por email.
     */
    UserEntityResponse getUserByEmail(String email);
}
