package com.digital.wallet.auth.service;

import com.digital.wallet.auth.config.JwtConfig;
import com.digital.wallet.user.domain.User;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@AllArgsConstructor
public class JwtService {
    private JwtConfig jwtConfig;

    public String generateToken(User user) {
        return null;
    }

    public boolean validateToken(String token) {
        return false;
    }
}
