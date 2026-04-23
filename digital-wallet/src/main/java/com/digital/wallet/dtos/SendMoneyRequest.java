package com.digital.wallet.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SendMoneyRequest {
    @NotNull(message = "Sender ID is required")
    private String senderId;

    @NotNull(message = "Receiver ID is required")
    private String receiverId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /**
     * IDEMPOTENCY KEY — yeh naya field hai
     *
     * Problem: Client ne request bheji, network timeout hua.
     * Client ne dobara bheji — ab paisa TWICE add ho gaya!
     *
     * Fix: Client ek unique requestId generate kare (UUID).
     * Agar same requestId dobara aaye → hum DB check karein → already done → skip.
     *
     * Real world mein: Razorpay, Stripe — sab idempotency keys use karte hain.
     */
    @NotBlank(message = "Idempotency key ID cannot be blank")
    private String idempotencyKey; // client generates this UUID — e.g. UUID.randomUUID()

    private String note;
}
