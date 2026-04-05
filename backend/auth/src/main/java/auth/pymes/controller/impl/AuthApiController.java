package auth.pymes.controller.impl;

import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.LogoutResponse;
import auth.pymes.controller.AuthApi;
import auth.pymes.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthApiController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> register(RegisterRequest request) {
        AuthResponse response = authService.register(request, getRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> login(LoginRequest request) {
        AuthResponse response = authService.login(request, getRequest());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    @Override
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String accessToken = null;

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        LogoutResponse response = authService.logout(accessToken);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
