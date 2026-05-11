package com.digital.wallet.services.impl;

import com.digital.wallet.dtos.AddMoneyRequest;
import com.digital.wallet.dtos.IdempotencyRecord;
import com.digital.wallet.dtos.SendMoneyRequest;
import com.digital.wallet.entities.Transaction;
import com.digital.wallet.entities.Wallet;
import com.digital.wallet.enums.TransactionStatus;
import com.digital.wallet.enums.TransactionType;
import com.digital.wallet.exceptions.ConflictException;
import com.digital.wallet.exceptions.InsufficientBalanceException;
import com.digital.wallet.exceptions.ResourceNotFoundException;
import com.digital.wallet.exceptions.WalletException;
import com.digital.wallet.repositories.TransactionRepo;
import com.digital.wallet.repositories.WalletRepo;
import com.digital.wallet.services.WalletService;
import com.digital.wallet.utils.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final WalletRepo walletRepo;
    private final TransactionRepo transactionRepo;

    public WalletServiceImpl(WalletRepo walletRepo, AuditService auditService,
                             TransactionRepo transactionRepo, IdempotencyService idempotencyService) {
        this.walletRepo = walletRepo;
        this.auditService = auditService;
        this.transactionRepo = transactionRepo;
        this.idempotencyService = idempotencyService;
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
    public String addMoney(AddMoneyRequest req, String idempotencyKey) {
        log.info("addMoney START userId={} amount={} requestId={}", req.getUserId(), req.getAmount());

        // ✅ Step 1: Cache check
        IdempotencyRecord existing = idempotencyService.getRecord(idempotencyKey);

        if(existing != null){
            if ("COMPLETED".equals(existing.getStatus())) {
                // Cache hit — wahi purana response return karo
                log.info("[IDEMPOTENCY] Cache hit, returning stored response, key={}", idempotencyKey);
                return existing.getTxnId();
            }

            if ("PROCESSING".equals(existing.getStatus())) {
                // Duplicate concurrent request
                throw new ConflictException("Request already in progress for key: " + idempotencyKey);
            }
        }

        // ✅ Step 2: PROCESSING mark karo — lock lo
        boolean lockAcquired = idempotencyService.markAsProcessing(idempotencyKey);

        if(!lockAcquired){
            // Koi aur thread pehle se processing kar raha hai
            throw new ConflictException("Request already in progress for key: " + idempotencyKey);
        }

        // ✅ Step 3: Actual wallet logic
        return processAddMoney(req, idempotencyKey);

    }

    private String processAddMoney(AddMoneyRequest req, String idempotencyKey){

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

            // ✅ Step 4: COMPLETED mark karo + TTL
            idempotencyService.markAsCompleted(idempotencyKey, txnId);

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

            // ✅ Step 5: Fail hua — Redis se hata do, retry fresh karega
            idempotencyService.deleteRecord(idempotencyKey);

            auditService.logTransaction(
                    txnId, null, req.getUserId(),
                    req.getAmount(), TransactionType.CREDIT,
                   "Added Money to wallet", TransactionStatus.FAILED
            );

            throw e;
        }
    }

    @Override
    @Transactional
    public String sendMoney(SendMoneyRequest req, String idempotencyKey) {

        log.info("sendMoney START senderId={} receiverId={} amount={} requestId={}",
                req.getSenderId(), req.getReceiverId(), req.getAmount());

        if (req.getSenderId().equals(req.getReceiverId())) {
            throw new WalletException("Sender and receiver cannot be same");
        }

        // ✅ Step 1: Cache check
        IdempotencyRecord existing = idempotencyService.getRecord(idempotencyKey);

        if(existing != null){
            if ("COMPLETED".equals(existing.getStatus())) {
                // Cache hit — wahi purana response return karo
                log.info("[IDEMPOTENCY] Cache hit, returning stored response, key={}", idempotencyKey);
                return existing.getTxnId();
            }

            if ("PROCESSING".equals(existing.getStatus())) {
                // Duplicate concurrent request
                throw new ConflictException("Request already in progress for key: " + idempotencyKey);
            }
        }

        // ✅ Step 2: PROCESSING mark karo — lock lo
        boolean lockAcquired = idempotencyService.markAsProcessing(idempotencyKey);

        if(!lockAcquired){
            // Koi aur thread pehle se processing kar raha hai
            throw new ConflictException("Request already in progress for key: " + idempotencyKey);
        }

        // ✅ Step 3: Process New Transaction
        return processSendMoney(req, idempotencyKey);

    }

    private String processSendMoney(SendMoneyRequest req, String idempotencyKey){
        log.info("sendMoney START senderId={} receiverId={} amount={}",
                req.getSenderId(), req.getReceiverId(), req.getAmount());

        // Common reference ID
        String referenceId = IdGenerator.generateTxnId();

        try{

            // 🔒 Deadlock prevention — always lock in sorted order
            /*
            * Scenario: User A sends ₹500 to User B, aur same time pe User B sends ₹200 to User A.
            * ❌ Without Fix — Deadlock Hoga
            * Thread 1 (A→B):                Thread 2 (B→A):
                Lock Wallet A 🔒               Lock Wallet B 🔒
                    |                               |
                    | Lock Wallet B chahiye...      | Lock Wallet A chahiye...
                    | ⏳ B locked hai, wait karo   | ⏳ A locked hai, wait karo
                    |                               |
                    forever wait...            forever wait... 💀
            *
            * ✅ With Fix — Sorted Order
            *   Rule: Hamesha alphabetical order mein lock karo.
                A < B, toh pehle A lock karo, phir B — chahe sender kaun bhi ho.
                * Thread 1 (A→B):                Thread 2 (B→A):
                Lock Wallet A 🔒               Lock Wallet A chahiye...
                Lock Wallet B 🔒               ⏳ A already locked, wait karo
                Complete ✅
                Release A, B 🔓
                                               Lock Wallet A 🔒
                                               Lock Wallet B 🔒
                                               Complete ✅
             */
            List<String> orderedIds = List.of(req.getSenderId(), req.getReceiverId())
                    .stream()
                    .sorted()
                    .toList();

            Wallet firstLocked  = walletRepo.findByUserIdForUpdate(orderedIds.get(0))
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + orderedIds.getFirst()));
            Wallet secondLocked = walletRepo.findByUserIdForUpdate(orderedIds.get(1))
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + orderedIds.get(1)));

            // Sort ke baad pata nahi kaun sender hai kaun receiver — map back karo
            Wallet sender   = firstLocked.getUserId().equals(req.getSenderId()) ? firstLocked : secondLocked;
            Wallet receiver = firstLocked.getUserId().equals(req.getReceiverId()) ? firstLocked : secondLocked;


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

            // ✅ Step 4: COMPLETED mark karo + TTL
            idempotencyService.markAsCompleted(idempotencyKey, referenceId);

            // 🧾 Debit log — sender ke liye
            auditService.logTransaction(
                    referenceId + "-D", sender.getUserId(), receiver.getUserId(),
                    req.getAmount(), TransactionType.DEBIT,
                    "Sent to user " + receiver.getUserId(), TransactionStatus.SUCCESS
            );

            // 🧾 Credit log — receiver ke liye
            auditService.logTransaction(
                    referenceId + "-C", sender.getUserId(), receiver.getUserId(),
                    req.getAmount(), TransactionType.CREDIT,
                    "Received from user " + sender.getUserId(), TransactionStatus.SUCCESS
            );

            log.info("sendMoney SUCCESS referenceId={} sender={} receiver={} amount={}",
                    referenceId, req.getSenderId(), req.getReceiverId(), req.getAmount());
            return referenceId;
        } catch (Exception e) {
            log.error("sendMoney FAILED sender={} receiver={} error={}", req.getSenderId(), req.getReceiverId(), e.getMessage());

            // ✅ Step 5: Fail hua — Redis se hata do, retry fresh karega
            idempotencyService.deleteRecord(idempotencyKey);

            // 🧾 FAILED log — REQUIRES_NEW se alag transaction mein save hoga
            auditService.logTransaction(
                    referenceId, req.getSenderId(), req.getReceiverId(),
                    req.getAmount(), TransactionType.DEBIT,
                    "Sent to user " + req.getReceiverId(), TransactionStatus.FAILED
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
