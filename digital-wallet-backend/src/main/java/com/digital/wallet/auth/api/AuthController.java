package com.digital.wallet.auth.api;

import com.digital.wallet.auth.dto.JwtResponse;
import com.digital.wallet.auth.dto.LogInRequest;
import com.digital.wallet.auth.dto.SignUpRequest;
import com.digital.wallet.auth.dto.SignUpResponse;
import com.digital.wallet.auth.exception.UserAlreadyExistsException;
import com.digital.wallet.auth.service.AuthService;
import com.digital.wallet.auth.service.JwtService;
import com.digital.wallet.user.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LogInRequest credentials){
        JwtResponse response = authService.loginWithCredential(credentials.email(), credentials.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signup(@Valid @RequestBody SignUpRequest request) throws UserAlreadyExistsException {
        SignUpResponse response = authService.signup(request);
        return ResponseEntity.ok(response);
    }

}
