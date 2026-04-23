package com.digital.wallet.entities;

import com.digital.wallet.enums.TransactionStatus;
import com.digital.wallet.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//TODO: Add Indexing

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

    @NotNull
    @Positive
    private BigDecimal amount;
    private String type;
    private String status;
    private String note;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(unique = true)
    private String idempotencyKey;
}
