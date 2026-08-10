package com.digital.wallet.transaction.api;

import com.digital.wallet.common.api.ApiErrorResponse;
import com.digital.wallet.common.api.ApiResponse;
import com.digital.wallet.common.util.IdGenerator;
import com.digital.wallet.transaction.domain.Transaction;
import com.digital.wallet.transaction.dto.MockTransactionDTO;
import com.digital.wallet.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transactions", description = "Transaction creation and history")
public class TransactionController {

    private final TransactionService txnService;

    public TransactionController(TransactionService txnService) {
        this.txnService = txnService;
    }

    @PostMapping
    @Operation(summary = "Create a mock transaction record")
    public ResponseEntity<ApiResponse<String>> createMockTransactions(@RequestBody MockTransactionDTO mockTransactionDTO){
        String txnId = IdGenerator.generateTxnId();
        txnService.saveTransaction(txnId,
                mockTransactionDTO.getSenderId(),
                mockTransactionDTO.getReceiverId(),
                mockTransactionDTO.getAmount(),
                mockTransactionDTO.getType(),
                mockTransactionDTO.getNote(),
                mockTransactionDTO.getStatus());
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Transaction created successfully", txnId)
        );
    }

    // 📜 Get all transactions for a user
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get a user's transaction history")
    public ResponseEntity<ApiResponse<List<Transaction>>> getUserTransactions(
            @PathVariable String userId) {
        List<Transaction> transactions = txnService.getUserTransactions(userId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Transactions fetched successfully", transactions)
        );
    }

    // 🔍 Get transaction by txnId
    @GetMapping("/{txnId}")
    @Operation(summary = "Get a transaction by its ID")
    public ResponseEntity<ApiResponse<Transaction>> getTransactionByTxnId(
            @PathVariable String txnId) {
        Transaction txn = txnService.getTransactionByTxnId(txnId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Transaction fetched successfully", txn)
        );
    }
}
