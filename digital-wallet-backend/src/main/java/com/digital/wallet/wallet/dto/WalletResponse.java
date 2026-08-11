package com.digital.wallet.wallet.dto;

import com.digital.wallet.wallet.domain.type.WalletStatus;

import java.math.BigDecimal;

public record WalletResponse (
    String id,
    String userId,
    BigDecimal balance,
    WalletStatus status
){}
