package com.digital.wallet.dtos;

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
    private Object responseBody; // null jab PROCESSING, full response jab COMPLETED
}