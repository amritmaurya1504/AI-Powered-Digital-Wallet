package com.digital.wallet.services.impl;

import com.digital.wallet.dtos.AddMoneyRequest;
import com.digital.wallet.dtos.SendMoneyRequest;
import com.digital.wallet.entities.Wallet;
import com.digital.wallet.enums.TransactionStatus;
import com.digital.wallet.enums.TransactionType;
import com.digital.wallet.exceptions.InsufficientBalanceException;
import com.digital.wallet.exceptions.ResourceNotFoundException;
import com.digital.wallet.exceptions.WalletException;
import com.digital.wallet.repositories.WalletRepo;
import com.digital.wallet.services.TransactionService;
import com.digital.wallet.services.WalletService;
import com.digital.wallet.utils.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final AuditService auditService;
    private final WalletRepo walletRepo;
    private final TransactionService txnService;

    public WalletServiceImpl(WalletRepo walletRepo, TransactionService txnService, AuditService auditService) {
        this.walletRepo = walletRepo;
        this.txnService = txnService;
        this.auditService = auditService;
    }

    @Override
    public Wallet createWallet(String userId) {
        log.info("Creating wallet for userId={}", userId);

        walletRepo.findByUserId(userId).ifPresent(w -> {
            throw new WalletException("Wallet already exists for user");
        });

        Wallet wallet = new Wallet();
        wallet.setId(IdGenerator.generateWalletId());
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        Wallet saved = walletRepo.save(wallet);
        log.info("Wallet created successfully walletId={} userId={}", saved.getId(), userId);
        return saved;
    }

    @Override
    @Transactional // 🔥 Ensures atomic transaction (all DB ops succeed or rollback)
    public String addMoney(AddMoneyRequest req) {
        log.info("addMoney START userId={} amount={}", req.getUserId(), req.getAmount());
        // 🔑 Generate txn id
        String txnId = IdGenerator.generateTxnId();

        try{
            // 🔒 Lock wallet
            Wallet wallet = walletRepo.findByUserIdForUpdate(req.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

            // 💰 Add amount
            wallet.setBalance(wallet.getBalance().add(req.getAmount()));

            // 💾 Save updated balance
            // ✅ Note: walletRepo.save() explicit call zaruri nahi hai @Transactional ke andar
            // Hibernate dirty checking automatically save karega — but explicit rakhna okay hai clarity ke liye
            walletRepo.save(wallet);

            // 🧾 Save CREDIT transaction
            auditService.logTransaction(
                    txnId, null, wallet.getUserId(),
                    req.getAmount(), TransactionType.CREDIT,
                    "Added Money to wallet", TransactionStatus.SUCCESS
            );


            log.info("addMoney SUCCESS txnId={} userId={} amount={}", txnId, req.getUserId(), req.getAmount());

            return txnId;

        } catch (Exception e) {
            log.error("addMoney FAILED userId={} amount={} error={}", req.getUserId(), req.getAmount(), e.getMessage());
            /**
             * auditService.logTransaction() — REQUIRES_NEW propagation wali method
             * Iska matlab: yeh apna ALAG transaction open karega.
             * Main transaction rollback ho → wallet update cancel
             * Audit transaction commit ho → FAILED log save rehti hai ✅
             */
            auditService.logTransaction(
                    txnId, null, req.getUserId(),
                    req.getAmount(), TransactionType.CREDIT,
                    e.getMessage(), TransactionStatus.FAILED
            );

            throw e;
        }

    }

    @Override
    @Transactional
    public String sendMoney(SendMoneyRequest req) {

        // ❌ Prevent self transfer
        if (req.getSenderId().equals(req.getReceiverId())) {
            throw new WalletException("Sender and receiver cannot be same");
        }

        // Common reference ID
        String referenceId = IdGenerator.generateTxnId();

        try{
            // 🔒 Lock sender wallet (prevents concurrent modification)
            Wallet sender = walletRepo.findByUserIdForUpdate(req.getSenderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

            // 🔒 Lock receiver wallet
            Wallet receiver = walletRepo.findByUserIdForUpdate(req.getReceiverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

            // ❌ Check sufficient balance
            if (sender.getBalance().compareTo(req.getAmount()) < 0) {
                throw new InsufficientBalanceException("Insufficient balance");
            }

            // 💸 Deduct from sender
            sender.setBalance(sender.getBalance().subtract(req.getAmount()));

            // 💰 Add to receiver
            receiver.setBalance(receiver.getBalance().add(req.getAmount()));

            // 💾 Save both wallets (still inside same transaction)
            walletRepo.save(sender);
            walletRepo.save(receiver);


            // 🧾 Log debit transaction
            txnService.saveTransaction(
                    referenceId + "-D",
                    sender.getUserId(),
                    receiver.getUserId(),
                    req.getAmount(),
                    "DEBIT",
                    "Sent to user " + receiver.getUserId(),
                    "SUCCESS"
            );

            // 🧾 Log credit transaction
            txnService.saveTransaction(
                    referenceId + "-C",
                    sender.getUserId(),
                    receiver.getUserId(),
                    req.getAmount(),
                    "CREDIT",
                    "Received from user " + sender.getUserId(),
                    "SUCCESS"
            );

            // ✅ Return txnId for tracking
            return referenceId;
        } catch (Exception e) {

            txnService.saveTransaction(
                    referenceId,
                    req.getSenderId(),
                    req.getReceiverId(),
                    req.getAmount(),
                    "DEBIT",
                    e.getMessage(),
                    "FAILED"
            );
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(String userId) {
        return walletRepo.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String userId) {
        return walletRepo.findByUserId(userId).map(Wallet::getBalance).orElse(BigDecimal.ZERO);
    }
}
