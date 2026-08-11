package com.digital.wallet.common.util;

import java.util.UUID;

public class IdGenerator {

    public static String generateTxnId() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateIdempotencyKey() {
        return "IDk-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
