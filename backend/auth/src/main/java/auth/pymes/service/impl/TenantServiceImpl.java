package auth.pymes.service.impl;

import auth.pymes.common.models.dto.request.CreateTenantRequest;
import auth.pymes.common.models.dto.request.SelectTenantRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserTenantResponse;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.PlanName;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.common.models.mappers.TenantMapper;
import auth.pymes.common.models.mappers.UserMapper;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.JwtService;
import auth.pymes.service.TenantService;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import auth.pymes.utils.exception.custom.InvalidInputException;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

import static auth.pymes.utils.exception.CodigoError.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl implements TenantService {

    private final UserEntityRepository userRepository;
    private final TenantRepository tenantRepository;
    private final UserTenantRepository userTenantRepository;
    private final JwtService jwtService;
    private final TenantMapper tenantMapper;
    private final UserMapper userMapper;

    @Override
    public Page<UserTenantResponse> getUserTenants(Pageable pageable, OAuth2User principal) {
        String email = principal.getAttribute("email");
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));

        Page<UserTenant> userTenants = userTenantRepository.findByUserIdAndIsActiveTrue(user.getId(), pageable);

        return userTenants.map(ut -> new UserTenantResponse(
                ut.getTenantId(),
                ut.getTenant().getName(),
                ut.getTenant().getSlug(),
                ut.getRole(),
                ut.getAcceptedAt() != null,
                ut.getAcceptedAt()
        ));
    }

    @Override
    @Transactional
    public AuthResponse selectTenant(SelectTenantRequest request, OAuth2User principal) {
        String email = principal.getAttribute("email");
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));

        UserTenant userTenant = userTenantRepository.findByUserIdAndTenantId(user.getId(), request.tenantId())
                .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT, request.tenantId()));

        if (!userTenant.getIsActive()) {
            throw new AuthorizationException(USER_NOT_IN_TENANT, "User access to tenant is inactive");
        }

        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException(TENANT_NOT_FOUND, request.tenantId()));

        if (!tenant.getIsActive()) {
            throw new AuthorizationException(TENANT_INACTIVE);
        }

        String accessToken = jwtService.generateAccessToken(user, tenant.getId(), userTenant.getRole().name(), tenant.getPlan().name());
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Usuario {} seleccionó tenant {} ({})", user.getEmail(), tenant.getName(), tenant.getId());

        return new AuthResponse(accessToken, refreshToken, userMapper.toResponse(user), tenantMapper.toResponse(tenant));
    }

    @Override
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request, OAuth2User principal) {
        String email = principal.getAttribute("email");
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));

        long freeTenantsCount = userTenantRepository.countByUserIdAndRole(user.getId(), RoleName.OWNER);
        if (freeTenantsCount >= 1) {
            throw new InvalidInputException(FREE_PLAN_LIMIT_REACHED);
        }

        if (tenantRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException(TENANT_ALREADY_EXISTS, request.slug());
        }

        Tenant tenant = Tenant.builder()
                .name(request.name())
                .slug(request.slug())
                .industry(request.industry())
                .plan(PlanName.FREE)
                .maxUsers(1)
                .isActive(true)
                .build();

        tenant = tenantRepository.save(tenant);

        UserTenant userTenant = UserTenant.builder()
                .userId(user.getId())
                .tenantId(tenant.getId())
                .role(RoleName.OWNER)
                .acceptedAt(ZonedDateTime.now())
                .isActive(true)
                .build();

        userTenantRepository.save(userTenant);

        log.info("Usuario {} creó tenant {} ({})", user.getEmail(), tenant.getName(), tenant.getId());

        return tenantMapper.toResponse(tenant);
    }
}
