package com.digital.wallet.utils;

import java.util.UUID;

public class IdGenerator {

    public static String generateTxnId() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public static String generateWalletId() {
        return "WAL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
