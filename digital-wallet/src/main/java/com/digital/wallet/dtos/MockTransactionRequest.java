package com.digital.wallet.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MockTransactionRequest {
    @NotNull(message = "Sender ID is required")
    private String senderId;

    @NotNull(message = "Receiver ID is required")
    private String receiverId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String note;

    private String type;
    private String status;
}
