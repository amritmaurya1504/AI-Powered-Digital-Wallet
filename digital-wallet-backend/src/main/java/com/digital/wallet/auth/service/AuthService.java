package com.digital.wallet.auth.service;

import com.digital.wallet.auth.dto.LoginRequest;
import com.digital.wallet.auth.dto.LoginResponse;
import com.digital.wallet.auth.dto.SignupResponse;
import com.digital.wallet.common.dto.CreateUserRequest;

public interface AuthService {
    SignupResponse signup(CreateUserRequest request);
    LoginResponse login(LoginRequest request);
}
