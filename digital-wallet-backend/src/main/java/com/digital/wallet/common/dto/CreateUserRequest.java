package com.digital.wallet.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank
        String name,
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
        String phone,
        @NotBlank
        @Size(min = 6, max = 16)
        String password
) {}