package com.digital.wallet.wallet.util;

import java.util.UUID;

public class IdGenerator {
    public static String generateTxnId() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "");
    }
    public static String generateWalletId() {
        return "WAL-" + UUID.randomUUID().toString().replace("-", "");
    }
}
