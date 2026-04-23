package com.digital.wallet.services.impl;

import com.digital.wallet.entities.Transaction;
import com.digital.wallet.exceptions.ResourceNotFoundException;
import com.digital.wallet.repositories.TransactionRepo;
import com.digital.wallet.services.TransactionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepo txnRepo;

    public TransactionServiceImpl(TransactionRepo txnRepo) {
        this.txnRepo = txnRepo;
    }

    @Override
    public void saveTransaction(String txnId, String senderId, String receiverId, BigDecimal amount, String type,
                                String note, String status, String idempotencyKey) {
        Transaction txn = new Transaction();
        txn.setId(txnId);
        txn.setSenderId(senderId);
        txn.setReceiverId(receiverId);
        txn.setAmount(amount);
        txn.setType(type);
        txn.setStatus(status);
        txn.setNote(note);
        txn.setIdempotencyKey(idempotencyKey);

        txnRepo.save(txn);
    }

    @Override
    public List<Transaction> getUserTransactions(String userId) {
        List<Transaction> transactions =
                txnRepo.findBySenderIdOrReceiverId(userId, userId);

        return transactions.stream()
                .filter(txn -> {

                    // 🔹 Case 1: User is sender → only DEBIT dikhao
                    if (txn.getSenderId() != null && txn.getSenderId().equals(userId)) {
                        return "DEBIT".equals(txn.getType());
                    }

                    // 🔹 Case 2: User is receiver → only CREDIT dikhao
                    if (txn.getReceiverId() != null && txn.getReceiverId().equals(userId)) {
                        return "CREDIT".equals(txn.getType());
                    }

                    return false;
                })
                .toList();
    }

    @Override
    public Transaction getTransactionByTxnId(String txnId) {
        return txnRepo.findById(txnId).orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }
}
