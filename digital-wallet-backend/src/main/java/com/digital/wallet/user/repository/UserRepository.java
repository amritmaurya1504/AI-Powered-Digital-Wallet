package com.digital.wallet.user.repository;

import com.digital.wallet.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmailId(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmailId(String email);
    boolean existsByPhone(String phone);
}
