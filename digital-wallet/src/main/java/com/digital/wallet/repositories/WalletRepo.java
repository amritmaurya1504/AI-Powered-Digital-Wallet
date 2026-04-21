package com.digital.wallet.repositories;

import com.digital.wallet.entities.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepo extends JpaRepository<Wallet, String> {
    Optional<Wallet> findByUserId(String userId);

    /**
     * Fetches Wallet by userId with a PESSIMISTIC WRITE lock.
     *
     * 🔒 What this does:
     * - Applies a database-level row lock on the selected wallet.
     * - Prevents other concurrent transactions from modifying this row
     *   until the current transaction is completed (commit/rollback).
     *
     * ⚙️ How it works internally:
     * - JPA translates this into: SELECT ... FOR UPDATE
     * - The row remains locked within the transaction boundary (@Transactional).
     *
     * 💡 Why we use this:
     * - To avoid race conditions during critical operations like money transfer.
     * - Ensures that balance updates are consistent and no double-spending occurs.
     *
     * ⚠️ Important:
     * - Must be used inside a @Transactional method, otherwise lock won't be effective.
     * - Should only be used for write operations (not for simple reads).
     *
     * 🧠 Example:
     * - Two requests try to update same wallet simultaneously
     * - First request acquires lock
     * - Second request waits until first completes
     *
     * @param userId unique identifier of the wallet owner
     * @return Optional containing Wallet if found, else empty
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") String userId);

    // Wallet ko database se lao aur usko lock bhi kar do
}
