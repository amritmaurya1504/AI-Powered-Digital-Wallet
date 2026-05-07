package com.digital.wallet;

import com.digital.wallet.dtos.AddMoneyRequest;
import com.digital.wallet.dtos.IdempotencyRecord;
import com.digital.wallet.dtos.SendMoneyRequest;
import com.digital.wallet.exceptions.InsufficientBalanceException;
import com.digital.wallet.exceptions.WalletException;
import com.digital.wallet.services.WalletService;
import com.digital.wallet.services.impl.IdempotencyService;
import com.digital.wallet.utils.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class SendMoneyIdempotencyTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private IdempotencyService idempotencyService;

    private String userA, userB, userC;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        userA = "user-A-" + ts;
        userB = "user-B-" + ts + 1;
        userC = "user-C-" + ts + 2;

        walletService.createWallet(userA);
        walletService.createWallet(userB);
        walletService.createWallet(userC);
    }

    private void addBalance(String userId, String amount) {
        AddMoneyRequest req = new AddMoneyRequest();
        req.setUserId(userId);
        req.setAmount(new BigDecimal(amount));
        walletService.addMoney(req, IdGenerator.generateIdempotencyKey());
    }

    private SendMoneyRequest buildSendRequest(String senderId, String receiverId, String amount) {
        SendMoneyRequest req = new SendMoneyRequest();
        req.setSenderId(senderId);
        req.setReceiverId(receiverId);
        req.setAmount(new BigDecimal(amount));
        return req;
    }

    // ================================================================
    // TEST 1 — Fresh request: Balance transfer ho, Redis mein store ho
    // ================================================================
    /**
     * SCENARIO:
     * A ke paas ₹1000, B ke paas ₹0
     * A → B ₹500 bhejta hai
     *
     * EXPECTED:
     * ✅ txnId return ho
     * ✅ A balance: ₹500
     * ✅ B balance: ₹500
     * ✅ Redis mein COMPLETED ho
     */
    @Test
    void sendMoney_freshRequest_shouldTransferAndStoreInRedis() {
        System.out.println("\n=== SEND MONEY FRESH REQUEST TEST ===");

        addBalance(userA, "1000");
        String key = IdGenerator.generateIdempotencyKey();
        SendMoneyRequest req = buildSendRequest(userA, userB, "500");

        System.out.println("Before: A=₹1000, B=₹0");

        String txnId = walletService.sendMoney(req, key);

        BigDecimal balA = walletService.getBalance(userA);
        BigDecimal balB = walletService.getBalance(userB);
        IdempotencyRecord record = idempotencyService.getRecord(key);

        System.out.println("TxnId: " + txnId);
        System.out.println("After: A=₹" + balA + ", B=₹" + balB);
        System.out.println("Redis status: " + record.getStatus());

        assertThat(txnId).isNotNull();
        assertThat(balA).isEqualByComparingTo("500");
        assertThat(balB).isEqualByComparingTo("500");
        assertThat(record.getStatus()).isEqualTo("COMPLETED");
        assertThat(record.getTxnId()).isEqualTo(txnId);

        // Total money conserved
        assertThat(balA.add(balB)).isEqualByComparingTo("1000");

        System.out.println("✅ Fresh send money processed correctly!");
    }

    // ================================================================
    // TEST 2 — Retry: Same key se dobara call, same txnId, no double debit
    // ================================================================
    /**
     * SCENARIO:
     * A → B ₹500, response aane se pehle crash
     * Client same key se retry karta hai
     *
     * EXPECTED:
     * ✅ Same txnId return ho
     * ✅ A balance sirf ek baar kate (₹500, not ₹0)
     * ✅ B balance sirf ek baar badhe (₹500, not ₹1000)
     */
    @Test
    void sendMoney_retryWithSameKey_shouldReturnSameTxnIdAndNotDoubleDebit() {
        System.out.println("\n=== SEND MONEY RETRY TEST ===");

        addBalance(userA, "1000");
        String key = IdGenerator.generateIdempotencyKey();
        SendMoneyRequest req = buildSendRequest(userA, userB, "500");

        String firstTxnId = walletService.sendMoney(req, key);
        System.out.println("First call txnId: " + firstTxnId);

        // Retry — same key
        String secondTxnId = walletService.sendMoney(req, key);
        System.out.println("Retry txnId: " + secondTxnId);

        BigDecimal balA = walletService.getBalance(userA);
        BigDecimal balB = walletService.getBalance(userB);

        System.out.println("After: A=₹" + balA + ", B=₹" + balB);

        assertThat(firstTxnId).isEqualTo(secondTxnId);
        assertThat(balA).isEqualByComparingTo("500"); // sirf ek baar kata
        assertThat(balB).isEqualByComparingTo("500"); // sirf ek baar badha

        System.out.println("✅ Retry handled — no double debit!");
    }

    // ================================================================
    // TEST 3 — Insufficient balance: Exception aaye, Redis clean ho
    // ================================================================
    /**
     * SCENARIO:
     * A ke paas sirf ₹100, lekin ₹500 bhejna chahta hai
     *
     * EXPECTED:
     * ✅ InsufficientBalanceException throw ho
     * ✅ Kisi ka balance na badhe na ghate
     * ✅ Redis mein PROCESSING record delete ho jaye
     */
    @Test
    void sendMoney_insufficientBalance_shouldThrowAndCleanRedis() {
        System.out.println("\n=== INSUFFICIENT BALANCE TEST ===");

        addBalance(userA, "100");
        String key = IdGenerator.generateIdempotencyKey();
        SendMoneyRequest req = buildSendRequest(userA, userB, "500");

        System.out.println("Before: A=₹100, B=₹0");

        assertThrows(
                InsufficientBalanceException.class,
                () -> walletService.sendMoney(req, key)
        );

        BigDecimal balA = walletService.getBalance(userA);
        BigDecimal balB = walletService.getBalance(userB);

        System.out.println("After: A=₹" + balA + ", B=₹" + balB);

        assertThat(balA).isEqualByComparingTo("100"); // unchanged
        assertThat(balB).isEqualByComparingTo("0");   // unchanged

        // Redis clean ho gaya — retry fresh start karega
        IdempotencyRecord record = idempotencyService.getRecord(key);
        assertThat(record).isNull();

        System.out.println("✅ Insufficient balance handled, Redis cleaned!");
    }

    // ================================================================
    // TEST 4 — Same sender receiver: Exception aaye
    // ================================================================
    /**
     * SCENARIO:
     * A apne aap ko paisa bhejna chahta hai
     *
     * EXPECTED:
     * ✅ WalletException throw ho
     */
    @Test
    void sendMoney_sameUser_shouldThrowWalletException() {
        System.out.println("\n=== SAME SENDER RECEIVER TEST ===");

        addBalance(userA, "1000");
        String key = IdGenerator.generateIdempotencyKey();
        SendMoneyRequest req = buildSendRequest(userA, userA, "500");

        assertThrows(
                WalletException.class,
                () -> walletService.sendMoney(req, key)
        );

        assertThat(walletService.getBalance(userA)).isEqualByComparingTo("1000");

        System.out.println("✅ Same sender-receiver rejected correctly!");
    }

    // ================================================================
    // TEST 5 — Concurrent same key: Sirf ek process ho
    // ================================================================
    /**
     * SCENARIO:
     * 2 threads same key se same time pe sendMoney karte hain
     *
     * EXPECTED:
     * ✅ Sirf 1 success
     * ✅ 1 conflict
     * ✅ Balance sirf ek baar kata
     */
    @Test
    void sendMoney_concurrentSameKey_onlyOneShouldSucceed() throws InterruptedException {
        System.out.println("\n=== CONCURRENT SAME KEY TEST ===");

        addBalance(userA, "1000");
        String key = IdGenerator.generateIdempotencyKey();
        SendMoneyRequest req = buildSendRequest(userA, userB, "500");

        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        executor.submit(() -> {
            try {
                startLatch.await();
                walletService.sendMoney(req, key);
                successCount.incrementAndGet();
                System.out.println("✅ Thread 1 SUCCESS");
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ Thread 1 FAILED: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                walletService.sendMoney(req, key);
                successCount.incrementAndGet();
                System.out.println("✅ Thread 2 SUCCESS");
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ Thread 2 FAILED: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown(); // 🚦 FIRE!
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal balA = walletService.getBalance(userA);
        BigDecimal balB = walletService.getBalance(userB);
        BigDecimal total = balA.add(balB);

        System.out.println("\n--- RESULTS ---");
        System.out.println("Success: " + successCount.get() + " | Failed: " + failCount.get());
        System.out.println("A=₹" + balA + " | B=₹" + balB + " | Total=₹" + total);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
        assertThat(balA).isEqualByComparingTo("500"); // sirf ek baar kata
        assertThat(total).isEqualByComparingTo("1000"); // money conserved

        System.out.println("✅ Concurrent duplicate handled!");
    }

    // ================================================================
    // TEST 6 — Deadlock prevention: A→B aur B→A same time
    // ================================================================
    /**
     * SCENARIO:
     * Thread 1: A → B ₹500
     * Thread 2: B → A ₹300
     * Dono same time pe — deadlock potential
     *
     * EXPECTED:
     * ✅ Dono complete ho jayein (alag keys hain)
     * ✅ Koi deadlock na ho
     * ✅ Total money conserved
     */
    @Test
    void sendMoney_bidirectionalConcurrent_shouldNotDeadlock() throws InterruptedException {
        System.out.println("\n=== DEADLOCK PREVENTION TEST ===");

        addBalance(userA, "1000");
        addBalance(userB, "1000");

        System.out.println("Before: A=₹1000, B=₹1000 | Total=₹2000");

        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Thread 1: A → B ₹500
        executor.submit(() -> {
            try {
                startLatch.await();
                walletService.sendMoney(
                        buildSendRequest(userA, userB, "500"),
                        IdGenerator.generateIdempotencyKey()  // alag key
                );
                successCount.incrementAndGet();
                System.out.println("✅ Thread 1: A → B ₹500 SUCCESS");
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ Thread 1 FAILED: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: B → A ₹300
        executor.submit(() -> {
            try {
                startLatch.await();
                walletService.sendMoney(
                        buildSendRequest(userB, userA, "300"),
                        IdGenerator.generateIdempotencyKey()  // alag key
                );
                successCount.incrementAndGet();
                System.out.println("✅ Thread 2: B → A ₹300 SUCCESS");
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ Thread 2 FAILED: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown(); // 🚦 FIRE!
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal balA = walletService.getBalance(userA);
        BigDecimal balB = walletService.getBalance(userB);
        BigDecimal total = balA.add(balB);

        System.out.println("\n--- RESULTS ---");
        System.out.println("Success: " + successCount.get() + " | Failed: " + failCount.get());
        System.out.println("A=₹" + balA + " | B=₹" + balB + " | Total=₹" + total);

        // Dono succeed hone chahiye — alag keys hain
        assertThat(successCount.get()).isEqualTo(2);

        // A: 1000 - 500 + 300 = 800
        assertThat(balA).isEqualByComparingTo("800");
        // B: 1000 + 500 - 300 = 1200
        assertThat(balB).isEqualByComparingTo("1200");

        // Total conserved
        assertThat(total).isEqualByComparingTo("2000");

        System.out.println("✅ No deadlock — both transactions completed!");
    }
}