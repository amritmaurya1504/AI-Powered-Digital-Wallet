package com.digital.wallet.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogInRequest(
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Size(min = 6, max = 20)
        String password
) {
}
