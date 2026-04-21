package auth.pymes.controller.impl;

import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LoginOauth2Controller {

    private final AuthService authService;

    @GetMapping("/login")
    public ResponseEntity<Void> loginPageRedirect(HttpServletRequest request) {
        String frontendUrl = System.getenv().getOrDefault("APP_FRONTEND_URL", "http://localhost:9200");
        return ResponseEntity.status(302)
                .header("Location", frontendUrl + "/#/login")
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}