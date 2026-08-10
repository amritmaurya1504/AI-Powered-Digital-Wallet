package com.digital.wallet.auth.service;

import com.digital.wallet.common.dto.CreateUserRequest;
import com.digital.wallet.common.dto.UserResponse;
import com.digital.wallet.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final UserService userService;

    @Override
    public UserResponse signup(CreateUserRequest request) {
        return userService.createUser(request);
    }
}
