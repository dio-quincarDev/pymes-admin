package auth.pymes.service.impl;

import auth.pymes.common.models.dto.response.MemberResponse;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.common.models.mappers.UserMapper;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.MemberService;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.InvalidInputException;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static auth.pymes.utils.exception.CodigoError.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final UserEntityRepository userRepository;
    private final TenantRepository tenantRepository;
    private final UserTenantRepository userTenantRepository;
    private final UserMapper userMapper;

    @Override
    public Page<MemberResponse> getTenantUsers(UUID tenantId, Pageable pageable, OAuth2User principal) {
        String requesterEmail = principal.getAttribute("email");
        UserEntity requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, requesterEmail));

        UserTenant requesterRelation = userTenantRepository.findByUserIdAndTenantId(requester.getId(), tenantId)
                .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT, tenantId));

        if (requesterRelation.getRole() != RoleName.OWNER && requesterRelation.getRole() != RoleName.ADMIN) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS);
        }

        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(TENANT_NOT_FOUND, tenantId));

        Page<UserTenant> userTenants = userTenantRepository.findByTenantIdAndIsActiveTrue(tenantId, pageable);

        return userTenants.map(ut -> new MemberResponse(
                userMapper.toResponse(ut.getUser()),
                ut.getRole(),
                ut.getAcceptedAt() != null,
                ut.getCreatedAt()
        ));
    }

    @Override
    @Transactional
    public MemberResponse updateUserRole(UUID tenantId, UUID userId, String newRole, OAuth2User principal) {
        String requesterEmail = principal.getAttribute("email");
        UserEntity requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, requesterEmail));

        UserTenant requesterRelation = userTenantRepository.findByUserIdAndTenantId(requester.getId(), tenantId)
                .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT, tenantId));

        if (requesterRelation.getRole() != RoleName.OWNER && requesterRelation.getRole() != RoleName.ADMIN) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS);
        }

        UserTenant targetRelation = userTenantRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_IN_TENANT, userId));

        RoleName targetRole = targetRelation.getRole();
        RoleName newRoleEnum;
        try {
            newRoleEnum = RoleName.valueOf(newRole);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(INVALID_ROLE, newRole);
        }

        if (!requesterRelation.getRole().hasMorePowerThan(targetRole)) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS,
                    "Cannot modify a user with role equal or higher than yours");
        }

        if (targetRole == RoleName.OWNER) {
            throw new AuthorizationException(OWNER_CANNOT_BE_REMOVED);
        }

        targetRelation.setRole(newRoleEnum);
        userTenantRepository.save(targetRelation);

        log.info("Usuario {} cambió rol de userId={} a {} en tenant {}",
                requester.getEmail(), userId, newRole, tenantId);

        return new MemberResponse(
                userMapper.toResponse(targetRelation.getUser()),
                targetRelation.getRole(),
                targetRelation.getAcceptedAt() != null,
                targetRelation.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public void deleteUserFromTenant(UUID tenantId, UUID userId, OAuth2User principal) {
        String requesterEmail = principal.getAttribute("email");
        UserEntity requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, requesterEmail));

        UserTenant requesterRelation = userTenantRepository.findByUserIdAndTenantId(requester.getId(), tenantId)
                .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT, tenantId));

        if (requesterRelation.getRole() != RoleName.OWNER) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS);
        }

        UserTenant targetRelation = userTenantRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_IN_TENANT, userId));

        if (targetRelation.getRole() == RoleName.OWNER) {
            throw new AuthorizationException(OWNER_CANNOT_BE_REMOVED);
        }

        if (requester.getId().equals(userId)) {
            throw new AuthorizationException(OWNER_CANNOT_BE_REMOVED,
                    "Owner cannot remove themselves. Transfer ownership first.");
        }

        userTenantRepository.delete(targetRelation);

        log.info("Usuario {} desvinculó userId={} del tenant {}", requester.getEmail(), userId, tenantId);
    }
}
