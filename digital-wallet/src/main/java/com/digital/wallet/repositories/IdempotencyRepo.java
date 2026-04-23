package com.digital.wallet.repositories;

import com.digital.wallet.entities.Idempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepo extends JpaRepository<Idempotency, Long> {
    Optional<Idempotency> findByIdempotencyKey(String key);
}
