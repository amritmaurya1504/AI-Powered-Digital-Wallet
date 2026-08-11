package com.digital.wallet.user.service;

import com.digital.wallet.common.dto.CreateUserRequest;
import com.digital.wallet.user.dto.UserResponse;
import com.digital.wallet.common.exception.ConflictException;
import com.digital.wallet.common.exception.ResourceNotFoundException;
import com.digital.wallet.common.util.MaskingUtils;
import com.digital.wallet.user.domain.User;
import com.digital.wallet.user.domain.type.RoleType;
import com.digital.wallet.user.dto.UpdateUserRequest;
import com.digital.wallet.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(CreateUserRequest request) {

        log.info(
                "USER_CREATION_STARTED email={} phone={}",
                request.email(),
                MaskingUtils.maskPhone(request.phone())
        );

        if(userRepository.existsByEmailId(request.email())) {
            log.warn(
                    "USER_CREATION_REJECTED reason=EMAIL_ALREADY_EXISTS email={}",
                    request.email()
            );
            throw new ConflictException("Email already registered");
        }

        if(userRepository.existsByPhone(request.phone())){
            log.warn(
                    "USER_CREATION_REJECTED reason=PHONE_ALREADY_EXISTS phone={}",
                    MaskingUtils.maskPhone(request.phone())

            );
            throw new ConflictException("This phone is associated with other account");
        }

        User user = User.builder()
                .emailId(request.email())
                .phone(request.phone())
                .name(request.name())
                .password(passwordEncoder.encode(request.password()))
                .roles(new HashSet<>(Set.of(RoleType.USER)))
                .build();

        try {
            User savedUser = userRepository.save(user);
            log.info(
                    "USER_CREATED userId={} email={}",
                    savedUser.getId(),
                    savedUser.getEmailId()
            );

            return this.mapToResponse(user);
        }catch (DataIntegrityViolationException ex){
            log.warn(
                    "USER_CREATION_REJECTED reason=UNIQUE_CONSTRAINT_VIOLATION email={} phone={}",
                    request.email(),
                    MaskingUtils.maskPhone(request.phone())
            );
            throw new ConflictException("Email or phone number already registered");
        }
    }

    @Override
    public List<UserResponse> getUsers() {
        log.info("FETCH_USERS_STARTED");
        List<UserResponse> users = userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
        log.info(
                "FETCH_USERS_COMPLETED count={}",
                users.size()
        );
        return users;
    }

    @Override
    public UserResponse getUserById(String userId) {
        log.info("FETCH_USER_STARTED userId={}", userId);
        User user = findUserById(userId);
        log.info("FETCH_USER_COMPLETED userId={}", userId);
        return this.mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        log.info("USER_UPDATE_STARTED userId={}", userId);
        User user = findUserById(userId);
        boolean updated = false;

        /*
         * Update email only when a new value was provided.
         */
        if(request.email() != null && !request.email().isBlank() && !request.email().equalsIgnoreCase(user.getEmailId())){
            if(userRepository.existsByEmailId(request.email())){
                log.warn("USER_UPDATE_REJECTED userId={} reason=EMAIL_ALREADY_EXISTS email={}", userId, request.email());
                throw new ConflictException("Email already registered");
            }
            user.setEmailId(request.email());
            updated = true;
        }

        /*
         * Update email only when a new value was provided.
         */
        if(request.phone() != null && !request.phone().isBlank() && !request.phone().equals(user.getPhone())){
            if(userRepository.existsByPhone(request.phone())){
                log.warn("USER_UPDATE_REJECTED userId={} reason=PHONE_ALREADY_EXISTS", userId);
                throw new ConflictException("Phone number already registered");
            }
            user.setPhone(request.phone());
            updated = true;
        }

        if(!updated){
            log.info("USER_UPDATE_SKIPPED userId={} reason=NO_CHANGES", userId);
            return mapToResponse(user);
        }

        try {
            User updatedUser = userRepository.save(user);
            log.info("USER_UPDATED userId={}", userId);
            return mapToResponse(updatedUser);
        } catch (DataIntegrityViolationException ex) {
            log.warn("USER_UPDATE_REJECTED userId={} reason=UNIQUE_CONSTRAINT_VIOLATION", userId);
            throw new ConflictException("Email or phone number already registered");
        }
    }

    @Override
    public void deleteUser(String userId) {
        log.info("USER_DELETION_STARTED userId={}", userId);
        User user = findUserById(userId);
        userRepository.delete(user);
        log.info("USER_DELETED userId={}", userId);
    }

    @Override
    public User findUserByEmail(String emailId) {
        return userRepository.findByEmailId(emailId).orElseThrow(() -> new UsernameNotFoundException(
                "username not found with id: " + emailId
        ));
    }

    private User findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("USER_NOT_FOUND userId={}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmailId(),
                user.getPhone(),
                user.getRoles()
        );
    }



}
