package auth.pymes.controller.impl;

import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.controller.UserApi;
import auth.pymes.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserApiController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<ApiResponse<UserEntityResponse>> getCurrentUser(Object principal) {
        UserEntityResponse user = userService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}
