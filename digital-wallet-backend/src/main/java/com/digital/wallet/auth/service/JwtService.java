package com.digital.wallet.auth.service;

import com.digital.wallet.auth.config.JwtConfig;
import com.digital.wallet.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;

    public String generateToken(User user){
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 10))
                .signWith(jwtConfig.getSecretKey())
                .compact();
    }

    public String getUserNameFromToken(String token){
        Claims claims = Jwts.parser()
                .verifyWith(jwtConfig.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

}
