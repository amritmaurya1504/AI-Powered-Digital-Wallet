package com.digital.wallet.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank
        String name,
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Size(min = 6, max = 20)
        String password,
        @NotBlank
        @Size(min = 10, max = 10)
        String phone
) {
}
