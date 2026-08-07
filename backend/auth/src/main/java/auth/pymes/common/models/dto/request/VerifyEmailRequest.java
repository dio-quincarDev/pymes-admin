package auth.pymes.common.models.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank(message = "Verification token is required")
        String token,

        @NotBlank(message = "Email is required")
        @jakarta.validation.constraints.Email(message = "Email must be valid")
        String email
) {}
