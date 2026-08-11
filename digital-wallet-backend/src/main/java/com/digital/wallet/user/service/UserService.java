package com.digital.wallet.user.service;

import com.digital.wallet.common.dto.CreateUserRequest;
import com.digital.wallet.user.domain.User;
import com.digital.wallet.user.dto.UserResponse;
import com.digital.wallet.user.dto.UpdateUserRequest;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest createUserRequest);
    List<UserResponse> getUsers();
    UserResponse getUserById(String userId);
    UserResponse updateUser(String userId, UpdateUserRequest updateUserRequest);
    void deleteUser(String userId);
    User findUserByEmail(String emailId);
}
