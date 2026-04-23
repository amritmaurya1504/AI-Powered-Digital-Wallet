package com.digital.wallet.entities;

import com.digital.wallet.enums.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency", uniqueConstraints = {
        @UniqueConstraint(columnNames = "idempotencyKey")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Idempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String idempotencyKey;

    private String txnId;

    @Column(columnDefinition = "TEXT")
    private String response;

    private String error;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private IdempotencyStatus status;
}
