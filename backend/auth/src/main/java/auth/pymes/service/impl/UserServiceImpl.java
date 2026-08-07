package auth.pymes.service.impl;

import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.mappers.UserMapper;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.UserService;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static auth.pymes.utils.exception.CodigoError.UNAUTHORIZED_ACCESS;
import static auth.pymes.utils.exception.CodigoError.USER_NOT_FOUND_BY_EMAIL;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserEntityRepository userRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantRepository tenantRepository;
    private final UserMapper userMapper;

    @Override
    public UserEntityResponse getCurrentUser(Object principal) {
        String email = extractEmail(principal);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));

        List<UserTenant> userTenants = userTenantRepository.findByUserIdAndIsActiveTrue(user.getId());
        UserTenant activeUt = userTenants.isEmpty() ? null : userTenants.get(0);
        UUID tenantId = activeUt != null ? activeUt.getTenantId() : null;
        String role = activeUt != null ? activeUt.getRole().name() : null;
        String plan = "FREE";
        if (tenantId != null) {
            Tenant t = tenantRepository.findById(tenantId).orElse(null);
            if (t != null) plan = t.getPlan().name();
        }

        return new UserEntityResponse(
                user.getId(), user.getEmail(), user.getName(),
                user.getPictureUrl(), user.getProvider(), tenantId, role, plan);
    }

    private String extractEmail(Object principal) {
        if (principal instanceof OAuth2User oAuth2User) {
            return oAuth2User.getAttribute("email");
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String email) {
            return email;
        }
        throw new AuthorizationException(UNAUTHORIZED_ACCESS, "Could not extract email from principal");
    }

    @Override
    public UserEntityResponse getUserByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));

        return new UserEntityResponse(
                user.getId(), user.getEmail(), user.getName(),
                user.getPictureUrl(), user.getProvider(), null, null, null);
    }
}
