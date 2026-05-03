package com.digital.wallet;

import com.digital.wallet.dtos.AddMoneyRequest;
import com.digital.wallet.dtos.SendMoneyRequest;
import com.digital.wallet.services.WalletService;
import com.digital.wallet.utils.IdGenerator;
import org.hibernate.validator.constraints.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
class FanInConcurrencyTest {

    Logger log = LoggerFactory.getLogger(FanInConcurrencyTest.class);

    @Autowired
    private WalletService walletService;

    // Sender IDs — 4 alag alag users
    private String senderA;
    private String senderB;
    private String senderC;
    private String senderD;

    // Ek hi receiver
    private String receiver;

    /**
     * @BeforeEach — har test se pehle fresh wallets banao
     *
     * Kyun? — Agar pehle test mein balance change hua toh
     * doosra test galat data pe chalega
     *
     * System.currentTimeMillis() — unique userId ensure karta hai
     * har test run pe alag timestamp → alag userId → naya wallet
     */
    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        senderA  = "sender-A-" + ts;
        senderB  = "sender-B-" + ts;
        senderC  = "sender-C-" + ts;
        senderD  = "sender-D-" + ts;
        receiver = "receiver-" + ts;

        // Sab wallets banao
        walletService.createWallet(senderA);
        walletService.createWallet(senderB);
        walletService.createWallet(senderC);
        walletService.createWallet(senderD);
        walletService.createWallet(receiver);

        // Har sender ko ₹1000 do
        addBalance(senderA, "1000");
        addBalance(senderB, "1000");
        addBalance(senderC, "1000");
        addBalance(senderD, "1000");

        // Receiver ka balance 0 hai — kuch add nahi kiya
    }

    // Helper method — balance add karna
    private void addBalance(String userId, String amount) {
        log.info("Add Balance Start");
        AddMoneyRequest req = new AddMoneyRequest();
        req.setUserId(userId);
        req.setAmount(new BigDecimal(amount));
        walletService.addMoney(req);
        log.info("Add Balance Exit");
    }

    // ================================================================
    // TEST 1 — 4 senders ek saath receiver ko ₹200 bhej rahe hain
    // ================================================================
    /**
     * SCENARIO:
     * Senderों ke paas ₹1000 each
     * Sab ek saath receiver ko ₹200 bhejte hain
     *
     * EXPECTED:
     * Receiver ka balance = 0 + (200 × successful senders)
     * Har sender ka balance = 1000 - 200 = ₹800 (agar success)
     *
     * Total money conserved rehni chahiye:
     * Before = 4×1000 + 0     = ₹4000
     * After  = 4×800  + 4×200 = ₹4000 ✅
     */
    @Test
    void test_4SendersSendToOneReceiver_simultaneously() throws InterruptedException {
        System.out.println("\n=== TEST 1: 4 Senders → 1 Receiver ===");

        String[] senders = {senderA, senderB, senderC, senderD};
        int senderCount = senders.length; // 4
        BigDecimal sendAmount = new BigDecimal("200");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(senderCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(senderCount);

        // Har sender ke liye ek thread
        for (String senderId : senders) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // sab ruko

                    SendMoneyRequest req = new SendMoneyRequest();
                    req.setSenderId(senderId);
                    req.setReceiverId(receiver);
                    req.setAmount(sendAmount);

                    walletService.sendMoney(req);
                    successCount.incrementAndGet();
                    System.out.println("✅ " + senderId + " → sent ₹200 to receiver");

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("❌ " + senderId + " → FAILED: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // FIRE! 🚦
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Results fetch karo
        BigDecimal receiverBalance = walletService.getBalance(receiver);
        BigDecimal balA = walletService.getBalance(senderA);
        BigDecimal balB = walletService.getBalance(senderB);
        BigDecimal balC = walletService.getBalance(senderC);
        BigDecimal balD = walletService.getBalance(senderD);

        BigDecimal totalAfter = receiverBalance.add(balA).add(balB).add(balC).add(balD);

        System.out.println("\n--- RESULTS ---");
        System.out.println("Success: " + successCount.get() + " | Failed: " + failCount.get());
        System.out.println("Sender A balance: ₹" + balA);
        System.out.println("Sender B balance: ₹" + balB);
        System.out.println("Sender C balance: ₹" + balC);
        System.out.println("Sender D balance: ₹" + balD);
        System.out.println("Receiver balance: ₹" + receiverBalance);
        System.out.println("Total (should be ₹4000): ₹" + totalAfter);

        // ✅ Total money conserved — koi paisa create/destroy nahi hua
        assertThat(totalAfter).isEqualByComparingTo(new BigDecimal("4000"));
        System.out.println("✅ Money conserved! No race condition.");
    }

    // ===============================================  =================
    // TEST 2 — Ek sender ke paas kam balance, baaki theek hain
    // ================================================================
    /**
     * SCENARIO:
     * SenderA ke paas sirf ₹50 hai (hum manually set karenge)
     * Sab ₹500 bhejne ki koshish karenge
     *
     * EXPECTED:
     * SenderA → FAIL (insufficient balance)
     * SenderB, C, D → SUCCESS
     *
     * Receiver balance = 3 × 500 = ₹1500
     */
    @Test
    void test_oneSenderInsufficientBalance_othersShouldSucceed() throws InterruptedException {
        System.out.println("\n=== TEST 2: One Sender Insufficient Balance ===");

        // SenderA ke paas sirf ₹50 hai — pehle ₹1000 add hua tha (setUp mein)
        // Toh pehle ₹950 nikaalo taaki ₹50 bache
        // Simplification: naya senderA banao with ₹50
        String poorSender = "poor-sender-" + System.currentTimeMillis();
        walletService.createWallet(poorSender);
        addBalance(poorSender, "50"); // sirf ₹50!

        String[] senders = {poorSender, senderB, senderC, senderD};
        int senderCount = senders.length;
        BigDecimal sendAmount = new BigDecimal("500");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(senderCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(senderCount);

        for (String senderId : senders) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    SendMoneyRequest req = new SendMoneyRequest();
                    req.setSenderId(senderId);
                    req.setReceiverId(receiver);
                    req.setAmount(sendAmount);

                    walletService.sendMoney(req);
                    successCount.incrementAndGet();
                    System.out.println("✅ " + senderId + " → sent ₹500");

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("❌ " + senderId + " → FAILED: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal receiverBalance  = walletService.getBalance(receiver);
        BigDecimal poorSenderBal    = walletService.getBalance(poorSender);

        System.out.println("\n--- RESULTS ---");
        System.out.println("Success: " + successCount.get() + " | Failed: " + failCount.get());
        System.out.println("Poor sender balance: ₹" + poorSenderBal + " (should still be ₹50)");
        System.out.println("Receiver balance: ₹" + receiverBalance);

        // poorSender ka balance unchanged rehna chahiye — uska transaction fail hua
        assertThat(poorSenderBal).isEqualByComparingTo(new BigDecimal("50"));

        // 3 success hone chahiye (B, C, D) — poorSender fail hoga
        assertThat(successCount.get()).isEqualTo(3);

        // Receiver ko 3 × 500 = ₹1500 milne chahiye
        assertThat(receiverBalance).isEqualByComparingTo(new BigDecimal("1500"));

        System.out.println("✅ Insufficient balance correctly handled!");
    }
}