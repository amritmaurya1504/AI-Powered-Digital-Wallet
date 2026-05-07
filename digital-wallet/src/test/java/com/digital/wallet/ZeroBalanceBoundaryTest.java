package com.digital.wallet;

import com.digital.wallet.dtos.AddMoneyRequest;
import com.digital.wallet.dtos.SendMoneyRequest;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ZeroBalanceBoundaryTest {

    @Autowired
    private WalletService walletService;

    private String userA, userB, userC;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        userA = "user-A-" + ts;
        userB = "user-B-" + ts;
        userC = "user-C-" + ts;

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

    // ================================================================
    // TEST — Exactly Zero Boundary
    // ================================================================
    /**
     * SCENARIO:
     * A ke paas exactly ₹500 hai
     *
     * Thread 1: A → B ₹500  (poora balance nikalna)
     * Thread 2: A → C ₹1    (ek rupaya — zero ke baad)
     *
     * Dono ek saath hit karte hain
     *
     * EXPECTED:
     * Sirf EK succeed hoga — dono nahi
     *
     * Case 1: Thread 1 pehle lock lega
     *    → A → B ₹500 SUCCESS → A balance = 0
     *    → Thread 2 → ₹1 send karne ki koshish → INSUFFICIENT ❌
     *
     * Case 2: Thread 2 pehle lock lega
     *    → A → C ₹1 SUCCESS → A balance = ₹499
     *    → Thread 1 → ₹500 send karne ki koshish → INSUFFICIENT ❌
     *
     * DONO CASES MEIN:
     * A ka balance kabhi NEGATIVE nahi hoga ✅
     * Total money conserved rehega ✅
     */
    @Test
    void test_exactZeroBoundary_negativeBalanceShouldNeverHappen() throws InterruptedException {
        System.out.println("\n=== ZERO BOUNDARY TEST ===");

        // A ke paas exactly ₹500
        addBalance(userA, "500");
        System.out.println("Before: A=₹500, B=₹0, C=₹0 | Total=₹500");

        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Thread 1: A → B ₹500 (poora balance!)
        executor.submit(() -> {
            try {
                startLatch.await();

                SendMoneyRequest req = new SendMoneyRequest();
                req.setSenderId(userA);
                req.setReceiverId(userB);
                req.setAmount(new BigDecimal("500")); // poora balance

                walletService.sendMoney(req,"");
                successCount.incrementAndGet();
                System.out.println("✅ Thread 1: A → B ₹500 SUCCESS");

            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ Thread 1: A → B ₹500 FAILED: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: A → C ₹1 (sirf ek rupaya — zero ke baad negative ho jaayega!)
        executor.submit(() -> {
            try {
                startLatch.await();

                SendMoneyRequest req = new SendMoneyRequest();
                req.setSenderId(userA);
                req.setReceiverId(userC);
                req.setAmount(new BigDecimal("1")); // sirf ₹1

                walletService.sendMoney(req, "");
                successCount.incrementAndGet();
                System.out.println("✅ Thread 2: A → C ₹1 SUCCESS");

            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ Thread 2: A → C ₹1 FAILED: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown(); // 🚦 FIRE!
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal balA = walletService.getBalance(userA);
        BigDecimal balB = walletService.getBalance(userB);
        BigDecimal balC = walletService.getBalance(userC);
        BigDecimal total = balA.add(balB).add(balC);

        System.out.println("\n--- RESULTS ---");
        System.out.println("Success: " + successCount.get() + " | Failed: " + failCount.get());
        System.out.println("Balance A: ₹" + balA);
        System.out.println("Balance B: ₹" + balB);
        System.out.println("Balance C: ₹" + balC);
        System.out.println("Total (should be ₹500): ₹" + total);

        // ✅ A ka balance kabhi negative nahi hona chahiye — EVER
        assertThat(balA)
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // ✅ Sirf 1 succeed hona chahiye — dono nahi
        assertThat(successCount.get()).isEqualTo(1);

        // ✅ Total money conserved
        assertThat(total).isEqualByComparingTo(new BigDecimal("500"));

        System.out.println("✅ Balance never went negative! Boundary condition handled correctly.");
    }
}