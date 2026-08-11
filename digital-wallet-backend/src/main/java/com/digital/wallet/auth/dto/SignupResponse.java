package com.digital.wallet.auth.dto;

import com.digital.wallet.user.dto.UserResponse;
import com.digital.wallet.wallet.dto.WalletResponse;

public record SignupResponse (
    UserResponse user,
    WalletResponse wallet
){}
