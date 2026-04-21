package com.digital.wallet.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "wallet",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wallet_user_id",  // constraint ka naam — useful for error messages
                columnNames = "user_id"
        )
)
public class Wallet {

    @Id
    private String id;
    private String userId;
    /* Because financial applications require high precision, and floating-point types
    like double can cause rounding errors
    Stores numbers exactly (no precision loss)
    Uses decimal arithmetic (not binary)
    Designed for:
        Banking
        Finance
        Payments
    */
    private BigDecimal balance = BigDecimal.ZERO;

}
