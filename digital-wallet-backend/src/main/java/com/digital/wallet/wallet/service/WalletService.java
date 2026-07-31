package com.digital.wallet.wallet.service;

import com.digital.wallet.wallet.domain.Wallet;
import com.digital.wallet.wallet.dto.AddMoneyRequest;
import com.digital.wallet.wallet.dto.SendMoneyRequest;

import java.math.BigDecimal;

public interface WalletService {

    Wallet createWallet(String userId);
    String addMoney(AddMoneyRequest req, String idempotencyKey);
    String sendMoney(SendMoneyRequest req, String idempotencyKey);
    Wallet getWalletByUserId(String userId);
    BigDecimal getBalance(String userId);

}
