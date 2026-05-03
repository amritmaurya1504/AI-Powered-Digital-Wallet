package com.digital.wallet.services.impl;

import com.digital.wallet.enums.TransactionStatus;
import com.digital.wallet.enums.TransactionType;
import com.digital.wallet.services.TransactionService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * WHY A SEPARATE AuditService?
 *
 * Yeh sabse important fix hai — original code ka sabse bada bug.
 *
 * PROBLEM (original code):
 * ┌─────────────────────────────────────────────────────┐
 * │  @Transactional                                      │
 * │  addMoney() {                                        │
 * │      try {                                           │
 * │          // wallet update...                         │
 * │      } catch (Exception e) {                         │
 * │          txnService.saveTransaction("FAILED") ← ❌   │
 * │          // ye bhi SAME transaction mein hai!        │
 * │          throw e; ← rollback trigger                 │
 * │          // FAILED record bhi rollback ho gaya!      │
 * │      }                                               │
 * │  }                                                   │
 * └─────────────────────────────────────────────────────┘
 *
 * Matlab jab bhi koi error aata, FAILED transaction log
 * save hi nahi hoti thi — silently disappear ho jaati thi!
 *
 * FIX — Propagation.REQUIRES_NEW:
 * ┌─────────────────────────────────────────────────────┐
 * │  Main Transaction (addMoney)                         │
 * │  ┌───────────────────────────────────────────────┐  │
 * │  │ ROLLBACK ho jaata hai — wallet update cancel  │  │
 * │  └───────────────────────────────────────────────┘  │
 * │                                                      │
 * │  Audit Transaction (REQUIRES_NEW — alag transaction) │
 * │  ┌───────────────────────────────────────────────┐  │
 * │  │ COMMIT hota hai — FAILED record save rehti hai│  │
 * │  └───────────────────────────────────────────────┘  │
 * └─────────────────────────────────────────────────────┘
 *
 * REQUIRES_NEW matlab: "Mujhe apna khud ka transaction chahiye,
 * parent ka suspend karo jab tak main complete na ho jaao"
 */
@Service
public class AuditService {

    private final TransactionService txnService;

    public AuditService(TransactionService txnService) {
        this.txnService = txnService;
    }

    /**
     * Propagation.REQUIRES_NEW — yeh parent transaction se bilkul alag hai.
     * Parent rollback ho toh bhi yeh commit hoga.
     * Parent commit ho toh bhi yeh independently commit hoga.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTransaction(
            String txnId,
            String senderId,
            String receiverId,
            BigDecimal amount,
            TransactionType type,
            String note,
            TransactionStatus status
    ) {
        txnService.saveTransaction(txnId, senderId, receiverId, amount,
                type.name(), note, status.name());
    }
}