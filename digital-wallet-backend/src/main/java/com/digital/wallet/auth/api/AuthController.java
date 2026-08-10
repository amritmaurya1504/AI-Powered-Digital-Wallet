package com.digital.wallet.auth.api;

import com.digital.wallet.auth.dto.LoginRequestDTO;
import com.digital.wallet.auth.dto.LoginResponseDTO;
import com.digital.wallet.auth.dto.SignupRequestDTO;
import com.digital.wallet.auth.dto.SignupResponseDTO;
import com.digital.wallet.auth.service.AuthService;
import com.digital.wallet.common.dto.CreateUserRequest;
import com.digital.wallet.common.dto.UserResponse;
import com.digital.wallet.common.util.MaskingUtils;
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
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDto){
        return ResponseEntity.ok(new LoginResponseDTO());
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody CreateUserRequest createUserRequest) {
        UserResponse res = authService.signup(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
}
