package com.digital.wallet.repositories;

import com.digital.wallet.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepo extends JpaRepository<Transaction, String> {
    List<Transaction> findBySenderIdOrReceiverId(String senderId, String receiverId);

    /**
     * ✅ Idempotency check ke liye
     *
     * "Kya yeh requestId pehle process ho chuki hai?"
     *
     * Spring Data JPA automatically query banayega:
     * SELECT * FROM transaction WHERE idempotency_key = ?
     *
     * Optional kyunki:
     * - Empty  → pehli baar aa raha hai → process karo
     * - Present → duplicate request → skip karo, same txnId return karo
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
