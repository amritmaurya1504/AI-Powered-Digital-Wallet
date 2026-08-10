package com.digital.wallet.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRequest(
        @Email
        String email,
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
        String phone
) {}
