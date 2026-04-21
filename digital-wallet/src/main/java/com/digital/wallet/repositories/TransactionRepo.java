package com.digital.wallet.repositories;

import com.digital.wallet.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepo extends JpaRepository<Transaction, String> {
    List<Transaction> findBySenderIdOrReceiverId(String senderId, String receiverId);
}
