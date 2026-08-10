package com.digital.wallet.wallet.api;

import com.digital.wallet.common.api.ApiResponse;
import com.digital.wallet.wallet.domain.Wallet;
import com.digital.wallet.wallet.dto.AddMoneyDTO;
import com.digital.wallet.wallet.dto.SendMoneyDTO;
import com.digital.wallet.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/wallet")
@Tag(name = "Wallets", description = "Wallet creation, balances, funding, and transfers")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    //Todo: After Auth module completed remove this
    @PostMapping("/create")
    @Operation(summary = "Create a wallet")
    public ResponseEntity<ApiResponse<Wallet>> createWallet(@RequestParam String userId) {
        Wallet wallet = walletService.createWallet(userId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Wallet created successfully", wallet)
        );
    }

    @PostMapping("/add-money")
    @Operation(summary = "Add mock funds to a wallet", description = "Supply a unique Idempotency-Key for each logical payment.")
    public ResponseEntity<ApiResponse<String>> addMoney(@RequestBody AddMoneyDTO req,
                                                        @Parameter(description = "Unique key used to safely retry the payment", required = true)
                                                        @RequestHeader(value = "Idempotency-Key", required = false)
                                                        String key) {
        String txnId = walletService.addMoney(req, key);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Amount added successfully", txnId)
        );
    }

    @PostMapping("/send-money")
    @Operation(summary = "Transfer money between wallets", description = "Supply a unique Idempotency-Key for each logical transfer.")
    public ResponseEntity<ApiResponse<String>> sendMoney(@RequestBody SendMoneyDTO req,
                                                         @Parameter(description = "Unique key used to safely retry the transfer", required = true)
                                                         @RequestHeader(value = "Idempotency-Key", required = false)
                                                         String key) {
        String txnId = walletService.sendMoney(req, key);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Money sent successfully", txnId)
        );
    }

    @GetMapping("/balance/{userId}")
    @Operation(summary = "Get a wallet balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(@PathVariable String userId) {
        BigDecimal balance = walletService.getBalance(userId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Balance fetched successfully", balance)
        );
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get wallet details")
    public ResponseEntity<ApiResponse<Wallet>> getWallet(@PathVariable String userId) {
        Wallet wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Wallet fetched successfully", wallet)
        );
    }

}
