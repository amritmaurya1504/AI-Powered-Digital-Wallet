package com.digital.wallet.user.service;

import com.digital.wallet.common.dto.CreateUserRequest;
import com.digital.wallet.common.dto.UserResponse;
import com.digital.wallet.user.dto.UpdateUserRequest;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest createUserRequest);
    List<UserResponse> getUsers();
    UserResponse getUserById(Long userId);
    UserResponse updateUser(Long userId, UpdateUserRequest updateUserRequest);
    void deleteUser(Long userId);
}
