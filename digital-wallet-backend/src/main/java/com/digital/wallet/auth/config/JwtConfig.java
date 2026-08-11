package com.digital.wallet.auth.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {
    @Value("${jwt.secretKey}")
    private String JWT_SECRET_KEY;

    public SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }
}
