package com.digital.wallet.services;

import com.digital.wallet.entities.Transaction;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {

    // 💾 Save transaction
    void saveTransaction(String txnId,
                         String senderId,
                         String receiverId,
                         BigDecimal amount,
                         String type,
                         String note,
                         String status);

    // 📜 Get all transactions of user
    List<Transaction> getUserTransactions(String userId);

    // 🔍 Get transaction by txnId
    Transaction getTransactionByTxnId(String txnId);

}
