package auth.pymes.unit;

import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.enums.AuthProvider;
import auth.pymes.common.models.mappers.UserMapper;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.impl.UserServiceImpl;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserEntityRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getCurrentUser_WithValidPrincipal_ReturnsUserResponse() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "test@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name("Test User")
                .build();
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(new UserEntityResponse(user.getId(), email, "Test User", null, AuthProvider.LOCAL, null, null, null));

        UserEntityResponse response = userService.getCurrentUser(principal);

        assertThat(response.email()).isEqualTo(email);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void getCurrentUser_WithUserDetailsPrincipal_ReturnsUserResponse() {
        String email = "userdetails@example.com";
        UserDetails principal = User.withUsername(email).password("ignored").authorities(Collections.emptyList()).build();

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name("UserDetails User")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(new UserEntityResponse(user.getId(), email, "UserDetails User", null, AuthProvider.LOCAL, null, null, null));

        UserEntityResponse response = userService.getCurrentUser(principal);

        assertThat(response.email()).isEqualTo(email);
    }

    @Test
    void getCurrentUser_WithStringPrincipal_ReturnsUserResponse() {
        String email = "string@example.com";

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name("String Principal User")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(new UserEntityResponse(user.getId(), email, "String Principal User", null, AuthProvider.LOCAL, null, null, null));

        UserEntityResponse response = userService.getCurrentUser(email);

        assertThat(response.email()).isEqualTo(email);
    }

    @Test
    void getUserByEmail_WithExistingEmail_ReturnsUserResponse() {
        String email = "test@example.com";
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(new UserEntityResponse(user.getId(), email, "Name", null, AuthProvider.LOCAL, null, null, null));

        UserEntityResponse response = userService.getUserByEmail(email);

        assertThat(response.email()).isEqualTo(email);
    }

    @Test
    void getUserByEmail_WhenEmailNotFound_ThrowsResourceNotFoundException() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail(email))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
