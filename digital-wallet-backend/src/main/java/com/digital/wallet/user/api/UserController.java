package com.digital.wallet.user.controller;

import com.digital.wallet.common.api.ApiResponse;
import com.digital.wallet.common.dto.CreateUserRequest;
import com.digital.wallet.user.dto.UpdateUserRequest;
import com.digital.wallet.user.dto.UserResponse;
import com.digital.wallet.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(
        name = "User Management",
        description = "APIs for managing digital wallet users"
)
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Create a new user",
            description = "Creates a new user account with the default USER role."
    )
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Get all users",
            description = "Returns all registered users."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully",
                userService.getUsers()));
    }

    @Operation(summary = "Get user by ID", description = "Returns a user using their unique user ID.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "Unique ID of the user", example = "USR-a81f92bc")
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully",
                userService.getUserById(userId)));
    }

    @Operation(summary = "Update user", description = "Updates the user's email or phone number.")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(
                    description = "Unique ID of the user",
                    example = "USR-a81f92bc"
            )
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(ApiResponse.success("User updated successfully",
                userService.updateUser(userId, request)));
    }

    @Operation(summary = "Delete user", description = "Deletes a user using their unique user ID.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(
                    description = "Unique ID of the user",
                    example = "USR-a81f92bc"
            )
            @PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}