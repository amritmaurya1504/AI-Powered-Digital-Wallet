package com.digital.wallet.wallet.idempotency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    private String key;       // idempotency key
    private String status;    // PROCESSING | COMPLETED
    private String txnId; // null jab PROCESSING, full response jab COMPLETED
}
