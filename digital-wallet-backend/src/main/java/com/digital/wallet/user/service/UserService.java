package com.digital.wallet.user.service;

import com.digital.wallet.common.dto.CreateUserRequest;
import com.digital.wallet.common.dto.UserResponse;
import com.digital.wallet.user.domain.User;
import com.digital.wallet.user.dto.UpdateUserRequest;

import java.util.List;
import java.util.Optional;

public interface UserService {
    UserResponse createUser(CreateUserRequest createUserRequest);
    List<UserResponse> getUsers();
    UserResponse getUserById(Long userId);
    UserResponse updateUser(Long userId, UpdateUserRequest updateUserRequest);
    void deleteUser(Long userId);

    Optional<User> getUserByEmail(String email);
}
