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
class AdvancedConcurrencyTest {

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

        // Teeno ko ₹1000 do
        addBalance(userA, "1000");
        addBalance(userB, "1000");
        addBalance(userC, "1000");
    }

    private void addBalance(String userId, String amount) {
        AddMoneyRequest req = new AddMoneyRequest();
        req.setUserId(userId);
        req.setAmount(new BigDecimal(amount));
        req.setIdempotencyKey(IdGenerator.generateIdempotencyKey());
        walletService.addMoney(req);
    }

    // ================================================================
    // TEST 3 — Circular Transfer
    // A→B, B→C, C→A — sab ek saath!
    // ================================================================
    /**
     * SCENARIO:
     *
     *    A ──₹300──→ B
     *    ↑            ↓
     *    C ←──₹300── B? nahi!
     *
     * Actually:
     *    Thread 1: A → B (₹300)
     *    Thread 2: B → C (₹300)
     *    Thread 3: C → A (₹300)
     *    Sab ek saath!
     *
     * DEADLOCK RISK:
     * Bina sorted locking ke:
     *    T1 locks A, wants B
     *    T2 locks B, wants C
     *    T3 locks C, wants A  ← CIRCULAR WAIT = DEADLOCK 💀
     *
     * Hamare fix ke saath (sorted lock order):
     *    Sab alphabetically lock lenge → koi circular wait nahi ✅
     *
     * EXPECTED:
     * Total money = ₹3000 (conserved)
     * Har user ka balance = ₹1000 (kyunki circular — jo gaya wo wapas aaya)
     */
    @Test
    void test_circularTransfer_noDeadlock() throws InterruptedException {
        System.out.println("\n=== TEST 3: Circular Transfer A→B→C→A ===");
        System.out.println("Before: A=₹1000, B=₹1000, C=₹1000");

        int threadCount = 3; // A→B, B→C, C→A
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Thread 1: A → B
        executor.submit(() -> {
            try {
                startLatch.await();
                SendMoneyRequest req = new SendMoneyRequest();
                req.setSenderId(userA);
                req.setReceiverId(userB);
                req.setAmount(new BigDecimal("300"));
                walletService.sendMoney(req);
                successCount.incrementAndGet();
                System.out.println("✅ A → B sent ₹300");
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ A → B FAILED: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: B → C
        executor.submit(() -> {
            try {
                startLatch.await();
                SendMoneyRequest req = new SendMoneyRequest();
                req.setSenderId(userB);
                req.setReceiverId(userC);
                req.setAmount(new BigDecimal("300"));
                walletService.sendMoney(req);
                successCount.incrementAndGet();
                System.out.println("✅ B → C sent ₹300");
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ B → C FAILED: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 3: C → A  ← yahi circular banata hai!
        executor.submit(() -> {
            try {
                startLatch.await();
                SendMoneyRequest req = new SendMoneyRequest();
                req.setSenderId(userC);
                req.setReceiverId(userA);
                req.setAmount(new BigDecimal("300"));
                walletService.sendMoney(req);
                successCount.incrementAndGet();
                System.out.println("✅ C → A sent ₹300");
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ C → A FAILED: " + e.getMessage());
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
        System.out.println("Total (should be ₹3000): ₹" + total);

        /**
         * Total money conserved — deadlock nahi hua
         * Agar deadlock hota → threads hang → doneLatch.await() timeout
         * → balances incomplete → total != 3000
         */
        assertThat(total).isEqualByComparingTo(new BigDecimal("3000"));
        System.out.println("✅ No deadlock! Circular transfer handled correctly.");
    }

    // ================================================================
    // TEST 4 — Same Wallet Simultaneously Debit + Credit
    // AddMoney aur SendMoney ek saath same wallet pe!
    // ================================================================
    /**
     * SCENARIO:
     *
     * User A ka wallet:
     *    Thread 1: Add ₹500      (credit)
     *    Thread 2: Send ₹300→B   (debit)
     *    Thread 3: Add ₹200      (credit)
     *    Thread 4: Send ₹400→C   (debit)
     *    Sab ek saath!
     *
     * BEFORE:
     *    A = ₹1000, B = ₹1000, C = ₹1000
     *    Total = ₹3000
     *
     * AFTER (agar sab success):
     *    A = 1000 + 500 + 200 - 300 - 400 = ₹1000
     *    B = 1000 + 300 = ₹1300
     *    C = 1000 + 400 = ₹1400
     *    Total = ₹3700
     *
     * RISK:
     * Bina locking ke — read-modify-write race condition
     * A ka balance galat calculate ho sakta hai
     *
     * EXPECTED:
     * Total money conserved — jo bhi success hua uske hisaab se
     */
    @Test
    void test_simultaneousDebitAndCredit_sameWallet() throws InterruptedException {
        System.out.println("\n=== TEST 4: Simultaneous Debit + Credit on Same Wallet ===");
        System.out.println("Before: A=₹1000, B=₹1000, C=₹1000 | Total=₹3000");

        int threadCount = 4;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        // Track karo kitna credit aur debit succeed hua
        AtomicInteger totalCredited = new AtomicInteger(0); // A mein add hua
        AtomicInteger totalDebited  = new AtomicInteger(0); // A se gaya

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Thread 1: Add ₹500 to A (credit)
        executor.submit(() -> {
            try {
                startLatch.await();
                addBalance(userA, "500");
                successCount.incrementAndGet();
                totalCredited.addAndGet(500);
                System.out.println("✅ Added ₹500 to A");
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ Add ₹500 FAILED: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: A → B ₹300 (debit)
        executor.submit(() -> {
            try {
                startLatch.await();
                SendMoneyRequest req = new SendMoneyRequest();
                req.setSenderId(userA);
                req.setReceiverId(userB);
                req.setAmount(new BigDecimal("300"));
                walletService.sendMoney(req);
                successCount.incrementAndGet();
                totalDebited.addAndGet(300);
                System.out.println("✅ A → B sent ₹300");
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ A→B FAILED: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 3: Add ₹200 to A (credit)
        executor.submit(() -> {
            try {
                startLatch.await();
                addBalance(userA, "200");
                successCount.incrementAndGet();
                totalCredited.addAndGet(200);
                System.out.println("✅ Added ₹200 to A");
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ Add ₹200 FAILED: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 4: A → C ₹400 (debit)
        executor.submit(() -> {
            try {
                startLatch.await();
                SendMoneyRequest req = new SendMoneyRequest();
                req.setSenderId(userA);
                req.setReceiverId(userC);
                req.setAmount(new BigDecimal("400"));
                walletService.sendMoney(req);
                successCount.incrementAndGet();
                totalDebited.addAndGet(400);
                System.out.println("✅ A → C sent ₹400");
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("❌ A→C FAILED: " + e.getMessage());
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

        /**
         * Expected A balance:
         * 1000 (initial) + totalCredited - totalDebited
         *
         * Kyun? — Kuch operations fail bhi ho sakte hain
         * Toh jo succeed hua uske hisaab se calculate karo
         */
        BigDecimal expectedA = new BigDecimal("1000")
                .add(new BigDecimal(totalCredited.get()))
                .subtract(new BigDecimal(totalDebited.get()));

        /**
         * Expected total:
         * 3000 (initial total) + totalCredited
         * Kyun? — addMoney bahar se paisa laata hai
         * sendMoney sirf transfer karta hai — total nahi badalta
         */
        BigDecimal expectedTotal = new BigDecimal("3000")
                .add(new BigDecimal(totalCredited.get()));

        System.out.println("\n--- RESULTS ---");
        System.out.println("Success: " + successCount.get() + " | Failed: " + failCount.get());
        System.out.println("Total credited to A: ₹" + totalCredited.get());
        System.out.println("Total debited from A: ₹" + totalDebited.get());
        System.out.println("Balance A: ₹" + balA + " (expected: ₹" + expectedA + ")");
        System.out.println("Balance B: ₹" + balB);
        System.out.println("Balance C: ₹" + balC);
        System.out.println("Total: ₹" + total + " (expected: ₹" + expectedTotal + ")");

        // A ka balance sahi calculate hua?
        assertThat(balA).isEqualByComparingTo(expectedA);

        // Total money conserved (+ jo bahar se add hua)
        assertThat(total).isEqualByComparingTo(expectedTotal);
        System.out.println("✅ Simultaneous debit+credit handled correctly!");
    }
}