package com.digital.wallet.controllers;

import com.digital.wallet.dtos.AddMoneyRequest;
import com.digital.wallet.dtos.ApiResponse;
import com.digital.wallet.dtos.SendMoneyRequest;
import com.digital.wallet.entities.Wallet;
import com.digital.wallet.services.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    // 🆕 Create Wallet
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Wallet>> createWallet(@RequestParam String userId) {
        Wallet wallet = walletService.createWallet(userId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Wallet created successfully", wallet)
        );
    }

    // 💰 Add Money
    @PostMapping("/add-money")
    public ResponseEntity<ApiResponse<String>> addMoney(@RequestBody AddMoneyRequest req,
                                                        @RequestHeader(value = "Idempotency-Key", required = false)
                                                        String key) {
        String txnId = walletService.addMoney(req, key);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Amount added successfully", txnId)
        );
    }

    // 💸 Send Money
    @PostMapping("/send-money")
    public ResponseEntity<ApiResponse<String>> sendMoney(@RequestBody SendMoneyRequest req) {
        String txnId = walletService.sendMoney(req);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Money sent successfully", txnId)
        );
    }

    // 💵 Get Balance
    @GetMapping("/balance/{userId}")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(@PathVariable String userId) {
        BigDecimal balance = walletService.getBalance(userId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Balance fetched successfully", balance)
        );
    }

    // 🔍 Get Wallet
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Wallet>> getWallet(@PathVariable String userId) {
        Wallet wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Wallet fetched successfully", wallet)
        );
    }

}
