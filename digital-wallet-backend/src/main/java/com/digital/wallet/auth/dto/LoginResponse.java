package com.digital.wallet.auth.dto;

public record LoginResponse (
        String userId,
        String jwt
){}
