package com.digital.wallet.auth.api;

import com.digital.wallet.auth.dto.LoginRequest;
import com.digital.wallet.auth.dto.LoginResponse;
import com.digital.wallet.auth.dto.SignupResponse;
import com.digital.wallet.auth.service.AuthService;
import com.digital.wallet.common.api.ApiResponse;
import com.digital.wallet.common.dto.CreateUserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequestDto){
        return ResponseEntity.ok(new ApiResponse<>(true, "Login success!", authService.login(loginRequestDto)));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody CreateUserRequest createUserRequest) {
        SignupResponse signupResponse = authService.signup(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Signup successfully!",
                signupResponse));
    }
}
