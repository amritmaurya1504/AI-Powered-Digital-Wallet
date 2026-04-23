package com.digital.wallet;

import com.digital.wallet.dtos.AddMoneyRequest;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AddMoneyIdempotencyTest {

    @Autowired
    private WalletService walletService;

    private String user;

    // ================================================================
    // SETUP — har test ke liye fresh wallet
    // ================================================================
    @BeforeEach
    void setUp() {
        user = "user-" + System.currentTimeMillis();
        walletService.createWallet(user);

        System.out.println("\n👤 Created wallet for: " + user);
    }

    // ================================================================
    // TEST 1 — Same idempotencyKey (concurrent requests)
    // ================================================================
    /**
     * SCENARIO:
     * 5 threads same request bhejte hain (same key)
     *
     * EXPECTED:
     * Sirf 1 baar paisa add hoga
     */
    @Test
    void test_addMoney_sameKey_concurrent_shouldCreditOnlyOnce() throws InterruptedException {
        System.out.println("\n=== TEST 1: SAME KEY (Concurrent AddMoney) ===");

        BigDecimal amount = new BigDecimal("500");
        String key = IdGenerator.generateIdempotencyKey(); // SAME KEY

        int threadCount = 5;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    AddMoneyRequest req = new AddMoneyRequest();
                    req.setUserId(user);
                    req.setAmount(amount);
                    req.setIdempotencyKey(key); // SAME KEY

                    walletService.addMoney(req);
                    System.out.println("✅ Add ₹500 attempt");

                } catch (Exception e) {
                    System.err.println("❌ FAILED: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 🚦 FIRE
        doneLatch.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal finalBalance = walletService.getBalance(user);

        System.out.println("\n--- RESULTS ---");
        System.out.println("Final Balance: ₹" + finalBalance);

        // ✅ ONLY ONE credit
        assertThat(finalBalance).isEqualByComparingTo(new BigDecimal("500"));

        System.out.println("✅ Idempotency working — duplicate ignored!");
    }

    // ================================================================
    // TEST 2 — Different idempotencyKey (normal behavior)
    // ================================================================
    /**
     * SCENARIO:
     * 5 threads → har request ka alag key
     *
     * EXPECTED:
     * Sab execute honge
     */
    @Test
    void test_addMoney_differentKeys_shouldCreditAll() throws InterruptedException {
        System.out.println("\n=== TEST 2: DIFFERENT KEYS ===");

        int threadCount = 5;
        BigDecimal amount = new BigDecimal("200");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    AddMoneyRequest req = new AddMoneyRequest();
                    req.setUserId(user);
                    req.setAmount(amount);
                    req.setIdempotencyKey(IdGenerator.generateIdempotencyKey()); // UNIQUE

                    walletService.addMoney(req);
                    System.out.println("✅ Added ₹200");

                } catch (Exception e) {
                    System.err.println("❌ FAILED: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal finalBalance = walletService.getBalance(user);

        System.out.println("\n--- RESULTS ---");
        System.out.println("Final Balance: ₹" + finalBalance);

        // ✅ 5 × 200 = 1000
        assertThat(finalBalance).isEqualByComparingTo(new BigDecimal("1000"));

        System.out.println("✅ All credits processed correctly!");
    }

    // ================================================================
    // TEST 3 — Same key sequential retry (simple case)
    // ================================================================
    /**
     * SCENARIO:
     * Same request 2 baar call (retry case)
     *
     * EXPECTED:
     * Sirf 1 baar paisa add hoga
     */
    @Test
    void test_addMoney_sameKey_sequential_shouldNotDuplicate() {
        System.out.println("\n=== TEST 3: SAME KEY (Sequential Retry) ===");

        String key = IdGenerator.generateIdempotencyKey();

        AddMoneyRequest req1 = new AddMoneyRequest();
        req1.setUserId(user);
        req1.setAmount(new BigDecimal("300"));
        req1.setIdempotencyKey(key);

        AddMoneyRequest req2 = new AddMoneyRequest();
        req2.setUserId(user);
        req2.setAmount(new BigDecimal("300"));
        req2.setIdempotencyKey(key);

        walletService.addMoney(req1);
        walletService.addMoney(req2); // retry

        BigDecimal finalBalance = walletService.getBalance(user);

        System.out.println("Final Balance: ₹" + finalBalance);

        // ✅ only one credit
        assertThat(finalBalance).isEqualByComparingTo(new BigDecimal("300"));

        System.out.println("✅ Sequential idempotency working!");
    }
}