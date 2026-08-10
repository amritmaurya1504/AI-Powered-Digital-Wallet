package com.digital.wallet.auth.service;

import com.digital.wallet.common.dto.CreateUserRequest;
import com.digital.wallet.common.dto.UserResponse;

public interface AuthService {
    UserResponse signup(CreateUserRequest request);
}
