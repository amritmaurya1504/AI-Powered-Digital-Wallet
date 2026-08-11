package com.digital.wallet.auth.service;

import com.digital.wallet.auth.dto.JwtResponse;
import com.digital.wallet.auth.dto.SignUpRequest;
import com.digital.wallet.auth.dto.SignUpResponse;
import com.digital.wallet.auth.exception.UserAlreadyExistsException;
import com.digital.wallet.common.dto.CreateUserRequest;
import com.digital.wallet.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Service
public class AuthService {
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public SignUpResponse signup(SignUpRequest request) throws UserAlreadyExistsException {
        var user = userService.getUserByEmail(request.email()).orElse(null);
        if(user != null) throw new UserAlreadyExistsException("Email already in use, try different email id.");

        String encryptedPassword = passwordEncoder.encode(request.password());
        var res = userService.createUser(new CreateUserRequest(request.name(), request.email(), request.phone(), encryptedPassword));

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", res.name());
        payload.put("email", res.email());

        String token = jwtService.generateTokenWithPayload(res.id().toString(), payload);

        return new SignUpResponse(res.id(), res.name(), res.email(), token);
    }

    public JwtResponse loginWithCredential(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        password
                )
        );

        var user = userService.getUserByEmail(email).orElseThrow();
        String token = jwtService.generateToken(user);
        return new JwtResponse(token);
    }
}
