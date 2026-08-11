package com.digital.wallet.user.dto;

import com.digital.wallet.user.domain.type.RoleType;
import java.util.Set;

public record UserResponse(
        String id,
        String email,
        String phone,
        Set<RoleType> roles
) {}
