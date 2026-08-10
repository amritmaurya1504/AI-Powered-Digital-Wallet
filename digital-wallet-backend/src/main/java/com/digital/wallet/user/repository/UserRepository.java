package com.digital.wallet.user.repository;

import com.digital.wallet.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
