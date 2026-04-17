package auth.pymes.common.models.mappers;

import auth.pymes.common.models.dto.response.InvitationResponse;
import auth.pymes.common.models.entities.Invitation;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InvitationMapper {

    @Mapping(target = "id", source = "invitation.id")
    @Mapping(target = "tenantId", source = "invitation.tenantId")
    @Mapping(target = "tenantName", source = "tenant.name")
    @Mapping(target = "email", source = "invitation.email")
    @Mapping(target = "role", source = "invitation.role")
    @Mapping(target = "invitedBy", source = "inviter.name")
    @Mapping(target = "invitedAt", source = "invitation.createdAt")
    @Mapping(target = "expiresAt", source = "invitation.expiresAt")
    @Mapping(target = "accepted", expression = "java(invitation.getAcceptedAt() != null)")
    InvitationResponse toResponse(Invitation invitation, Tenant tenant, UserEntity inviter);
}
