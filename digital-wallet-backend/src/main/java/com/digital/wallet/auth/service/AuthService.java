package com.digital.wallet.auth.service;

import com.digital.wallet.auth.dto.JwtResponse;
import com.digital.wallet.auth.dto.SignUpRequest;
import com.digital.wallet.auth.dto.SignUpResponse;
import com.digital.wallet.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class AuthService {
    private final UserService userService;

    public SignUpResponse signup(SignUpRequest request) {
        return null;
    }

    public JwtResponse loginWithCredential(String email, String password) {
        return null;
    }
}
