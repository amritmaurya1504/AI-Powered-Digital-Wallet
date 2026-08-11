package com.digital.wallet.auth.service;

import com.digital.wallet.auth.config.JwtConfig;
import com.digital.wallet.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class JwtService {
    private JwtConfig jwtConfig;

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("name", user.getName());
        claims.put("email", user.getEmailId());

        return generateTokenWithPayload(user.getId().toString(), claims);
    }

    public String generateTokenWithPayload(String subject, Map<String, Object> payload) {
        return Jwts.builder()
                .signWith(jwtConfig.getSecretKey())
                .subject(subject)
                .claims(payload)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * jwtConfig.getExpiration()))
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtConfig.getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
