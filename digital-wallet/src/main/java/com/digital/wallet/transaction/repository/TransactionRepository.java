package com.digital.wallet.transaction.repository;

import com.digital.wallet.transaction.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findBySenderIdOrReceiverId(String senderId, String receiverId);
}
