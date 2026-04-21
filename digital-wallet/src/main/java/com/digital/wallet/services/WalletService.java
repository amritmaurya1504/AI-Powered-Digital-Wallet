package com.digital.wallet.services;

import com.digital.wallet.dtos.AddMoneyRequest;
import com.digital.wallet.dtos.SendMoneyRequest;
import com.digital.wallet.entities.Wallet;

import java.math.BigDecimal;

public interface WalletService {

    Wallet createWallet(String userId);
    String addMoney(AddMoneyRequest req);
    String sendMoney(SendMoneyRequest req);
    Wallet getWalletByUserId(String userId);
    BigDecimal getBalance(String userId);

}
