package com.digital.wallet.common.dto;

import com.digital.wallet.user.domain.type.RoleType;
import java.util.Set;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Set<RoleType> roles
) {}
