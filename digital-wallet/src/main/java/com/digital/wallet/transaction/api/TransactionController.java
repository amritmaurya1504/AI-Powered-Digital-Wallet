package com.digital.wallet.transaction.api;

import com.digital.wallet.common.api.ApiResponse;
import com.digital.wallet.common.util.IdGenerator;
import com.digital.wallet.transaction.domain.Transaction;
import com.digital.wallet.transaction.dto.MockTransactionRequest;
import com.digital.wallet.transaction.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService txnService;

    public TransactionController(TransactionService txnService) {
        this.txnService = txnService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createMockTransactions(@RequestBody MockTransactionRequest mockTransactionRequest){
        String txnId = IdGenerator.generateTxnId();
        txnService.saveTransaction(txnId,
                mockTransactionRequest.getSenderId(),
                mockTransactionRequest.getReceiverId(),
                mockTransactionRequest.getAmount(),
                mockTransactionRequest.getType(),
                mockTransactionRequest.getNote(),
                mockTransactionRequest.getStatus());
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Transaction created successfully", txnId)
        );
    }

    // 📜 Get all transactions for a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Transaction>>> getUserTransactions(
            @PathVariable String userId) {
        List<Transaction> transactions = txnService.getUserTransactions(userId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Transactions fetched successfully", transactions)
        );
    }

    // 🔍 Get transaction by txnId
    @GetMapping("/{txnId}")
    public ResponseEntity<ApiResponse<Transaction>> getTransactionByTxnId(
            @PathVariable String txnId) {
        Transaction txn = txnService.getTransactionByTxnId(txnId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Transaction fetched successfully", txn)
        );
    }
}
