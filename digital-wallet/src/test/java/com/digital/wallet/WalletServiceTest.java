package com.digital.wallet;

import com.digital.wallet.dtos.AddMoneyRequest;
import com.digital.wallet.services.impl.IdempotencyService;
import com.digital.wallet.services.WalletService;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IdempotencyTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private IdempotencyService idempotencyService;

    private String userId;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        userId = "user-" + ts;
        walletService.createWallet(userId);
    }

    private AddMoneyRequest buildRequest(String userId, String amount) {
        AddMoneyRequest req = new AddMoneyRequest();
        req.setUserId(userId);
        req.setAmount(new BigDecimal(amount));
        return req;
    }

    // ================================================================
    // TEST 1 — Fresh request: Redis mein store ho, balance badhe
    // ================================================================
    /**
     * SCENARIO:
     * Bilkul naya idempotency key — cache mein kuch nahi
     *
     * EXPECTED:
     * ✅ txnId return ho
     * ✅ Balance 0 → 500 ho jaye
     * ✅ Redis mein COMPLETED record store ho
     */
    @Test
    void test_freshRequest_shouldProcessAndStoreInRedis() {
        System.out.println("\n=== FRESH REQUEST TEST ===");

        String key = IdGenerator.generateIdempotencyKey();
        AddMoneyRequest req = buildRequest(userId, "500");

        System.out.println("Before: balance=₹0, redis=empty");

        String txnId = walletService.addMoney(req, key);

        BigDecimal balance = walletService.getBalance(userId);
        var record = idempotencyService.getRecord(key);

        System.out.println("TxnId: " + txnId);
        System.out.println("Balance: ₹" + balance);
        System.out.println("Redis status: " + record.getStatus());

        assertThat(txnId).isNotNull();
        assertThat(balance).isEqualByComparingTo("500");
        assertThat(record).isNotNull();
        assertThat(record.getStatus()).isEqualTo("COMPLETED");
        assertThat(record.getTxnId()).isEqualTo(txnId);

        System.out.println("✅ Fresh request processed correctly!");
    }

    // ================================================================
    // TEST 2 — Retry: Same key se dobara call, same txnId, no double credit
    // ================================================================
    /**
     * SCENARIO:
     * Pehli request gayi, response aane se pehle client crash
     * Client retry karta hai same idempotency key se
     *
     * EXPECTED:
     * ✅ Same txnId return ho
     * ✅ Balance sirf ek baar badhe (500, not 1000)
     * ✅ No double credit
     */
    @Test
    void test_retryWithSameKey_shouldReturnSameTxnIdAndNotDoubleCredit() {
        System.out.println("\n=== RETRY TEST ===");

        String key = IdGenerator.generateIdempotencyKey();
        AddMoneyRequest req = buildRequest(userId, "500");

        String firstTxnId = walletService.addMoney(req, key);
        System.out.println("First call txnId: " + firstTxnId);
        System.out.println("Balance after first: ₹" + walletService.getBalance(userId));

        // Retry — same key
        String secondTxnId = walletService.addMoney(req, key);
        System.out.println("Retry txnId: " + secondTxnId);

        BigDecimal balance = walletService.getBalance(userId);
        System.out.println("Balance after retry: ₹" + balance);

        // Same txnId aana chahiye
        assertThat(firstTxnId).isEqualTo(secondTxnId);

        // Double credit nahi hona chahiye
        assertThat(balance).isEqualByComparingTo("500"); // 500, NOT 1000

        System.out.println("✅ Retry handled correctly — no double credit!");
    }

    // ================================================================
    // TEST 3 — Alag keys: Alag alag txnId, balance dono baar badhe
    // ================================================================
    /**
     * SCENARIO:
     * Do alag requests — alag idempotency keys
     * Dono genuine nayi requests hain
     *
     * EXPECTED:
     * ✅ Alag alag txnId
     * ✅ Balance dono baar badhe (500 + 500 = 1000)
     */
    @Test
    void test_differentKeys_shouldProcessBothAndGivesDifferentTxnIds() {
        System.out.println("\n=== DIFFERENT KEYS TEST ===");

        AddMoneyRequest req = buildRequest(userId, "500");

        String txnId1 = walletService.addMoney(req, IdGenerator.generateIdempotencyKey());
        String txnId2 = walletService.addMoney(req, IdGenerator.generateIdempotencyKey());

        BigDecimal balance = walletService.getBalance(userId);

        System.out.println("TxnId 1: " + txnId1);
        System.out.println("TxnId 2: " + txnId2);
        System.out.println("Final balance: ₹" + balance);

        assertThat(txnId1).isNotEqualTo(txnId2);
        assertThat(balance).isEqualByComparingTo("1000"); // 500 + 500

        System.out.println("✅ Both requests processed independently!");
    }

    // ================================================================
    // TEST 4 — Concurrent same key: Sirf ek process ho, ek conflict le
    // ================================================================
    /**
     * SCENARIO:
     * 2 threads ek saath same idempotency key se addMoney karte hain
     * (race condition — real world retry simulation)
     *
     * EXPECTED:
     * ✅ Sirf 1 success ho
     * ✅ 1 conflict/exception ho
     * ✅ Balance sirf ek baar badhe (500, not 1000)
     * ✅ Redis mein sirf ek COMPLETED record ho
     */
    @Test
    void test_concurrentSameKey_onlyOneShouldSucceed() throws InterruptedException {
        System.out.println("\n=== CONCURRENT SAME KEY TEST ===");

        String key = IdGenerator.generateIdempotencyKey();
        AddMoneyRequest req = buildRequest(userId, "500");

        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicReference<String> txnIdRef = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Thread 1 — same key
        executor.submit(() -> {
            try {
                startLatch.await();
                String txnId = walletService.addMoney(req, key);
                txnIdRef.set(txnId);
                successCount.incrementAndGet();
                System.out.println("✅ Thread 1 SUCCESS txnId=" + txnId);
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ Thread 1 FAILED: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2 — same key (duplicate concurrent request)
        executor.submit(() -> {
            try {
                startLatch.await();
                String txnId = walletService.addMoney(req, key);
                successCount.incrementAndGet();
                System.out.println("✅ Thread 2 SUCCESS txnId=" + txnId);
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

        BigDecimal balance = walletService.getBalance(userId);
        var record = idempotencyService.getRecord(key);

        System.out.println("\n--- RESULTS ---");
        System.out.println("Success: " + successCount.get() + " | Failed: " + failCount.get());
        System.out.println("Balance: ₹" + balance);
        System.out.println("Redis status: " + (record != null ? record.getStatus() : "null"));

        // Sirf 1 succeed hona chahiye
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);

        // Balance sirf ek baar badha
        assertThat(balance).isEqualByComparingTo("500"); // NOT 1000

        // Redis mein COMPLETED record hai
        assertThat(record).isNotNull();
        assertThat(record.getStatus()).isEqualTo("COMPLETED");

        System.out.println("✅ Concurrent duplicate handled — no double credit!");
    }
}