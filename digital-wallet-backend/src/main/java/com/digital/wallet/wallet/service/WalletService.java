package com.digital.wallet.wallet.service;

import com.digital.wallet.wallet.domain.Wallet;
import com.digital.wallet.wallet.dto.AddMoneyDTO;
import com.digital.wallet.wallet.dto.SendMoneyDTO;
import com.digital.wallet.wallet.dto.WalletResponse;

import java.math.BigDecimal;

public interface WalletService {

    WalletResponse createWallet(String userId);
    String addMoney(AddMoneyDTO req, String idempotencyKey);
    String sendMoney(SendMoneyDTO req, String idempotencyKey);
    Wallet getWalletByUserId(String userId);
    BigDecimal getBalance(String userId);

}
