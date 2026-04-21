package com.digital.wallet.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    private String id;
    private String senderId;
    private String receiverId;
    private BigDecimal amount;
    private String type;
    private String status;
    private String note;
    private LocalDateTime createdAt = LocalDateTime.now();
}
