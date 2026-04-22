package com.digital.wallet;

import com.digital.wallet.dtos.AddMoneyRequest;
import com.digital.wallet.services.WalletService;
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
class DigitalWalletApplicationTests {

	@Autowired
	private WalletService walletService;

	@Test
	public void testConcurrentAddMoney() throws InterruptedException {

		// =====================================================
		// STEP 1 — SETUP
		// =====================================================

		/**
		 * Ek fresh wallet banao har test run pe
		 * System.currentTimeMillis() isliye — har test mein unique userId chahiye
		 * Warna agar pehle se wallet hai toh createWallet fail karega
		 *
		 * Example: "test-user-add-1719123456789"
		 */
		String userId = "test-user-add-" + System.currentTimeMillis();
		walletService.createWallet(userId);

		// 10 threads ek saath ₹100 add karenge
		int threadCount = 10;
		BigDecimal amountPerThread = new BigDecimal("100");

		// =====================================================
		// STEP 2 — LATCHES BANAO
		// =====================================================

		/**
		 * CountDownLatch(1) — startLatch
		 *
		 * Socho ek race ka starter pistol 🔫
		 * new CountDownLatch(1) — matlab "1 baar countdown hoga"
		 *
		 * Sab threads startLatch.await() pe rukenge
		 * Jab hum startLatch.countDown() karenge — sab ek saath chhoot jayenge
		 *
		 * Yeh kyun? — Agar latch na ho toh threads ek ek karke start honge
		 * Hume sab ko EXACTLY ek saath start karna hai — concurrency simulate karne ke liye
		 */
		CountDownLatch startLatch = new CountDownLatch(1);

		/**
		 * CountDownLatch(threadCount) — doneLatch
		 *
		 * Yeh "sab kaam khatam hone ka signal" hai
		 * har thread jab complete hogi → doneLatch.countDown() karegi
		 * 10 threads → 10 baar countDown → latch 0 pe aayega
		 *
		 * Main thread doneLatch.await() pe rukegi
		 * Jab sab 10 threads done → main thread aage badhegi
		 */
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		// =====================================================
		// STEP 3 — COUNTERS BANAO
		// =====================================================

		/**
		 * AtomicInteger — thread-safe counter
		 *
		 * Normal int kyun nahi?
		 * int count = 0;
		 * count++ — yeh 3 steps mein hota hai internally:
		 *   1. count ki value padho
		 *   2. 1 add karo
		 *   3. wapas store karo
		 *
		 * 2 threads ek saath count++ karein toh:
		 *   Thread A reads 0
		 *   Thread B reads 0  (A ne abhi store nahi kiya!)
		 *   Thread A stores 1
		 *   Thread B stores 1  ← count 2 hona chahiye tha, 1 hai!
		 *
		 * AtomicInteger — yeh teen steps atomic hain (ek saath hote hain)
		 * koi dusra thread beech mein nahi aa sakta → safe hai ✅
		 */
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);

		// =====================================================
		// STEP 4 — THREAD POOL BANAO
		// =====================================================

		/**
		 * ExecutorService — thread pool manager
		 *
		 * Executors.newFixedThreadPool(10) — exactly 10 threads ka pool banao
		 *
		 * Thread pool kyun? — Khud new Thread() banana expensive hota hai
		 * Pool mein threads ready rehte hain — reuse hote hain
		 *
		 * Visualize karo:
		 * Pool = ek office 🏢
		 * Threads = 10 employees 👨‍💼👨‍💼👨‍💼...
		 * submit() = "yeh kaam karo"
		 * Sab employees ko ek saath kaam milega
		 */
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		// =====================================================
		// STEP 5 — TASKS SUBMIT KARO
		// =====================================================

		for (int i = 0; i < threadCount; i++) {

			/**
			 * executor.submit() — ek kaam pool mein daalo
			 * Lambda () -> {...} — yeh kaam hai jo thread karegi
			 *
			 * Yeh loop 10 baar chalega → 10 tasks submit honge
			 * 10 threads inhe uthayengi aur run karengi
			 */
			executor.submit(() -> {

				try {
					/**
					 * startLatch.await() — RUKOO!
					 *
					 * Har thread yahan aa ke ruk jaayegi
					 * Jab tak startLatch.countDown() na ho — koi aage nahi badhega
					 *
					 * Yeh ensure karta hai ki sab threads simultaneously
					 * addMoney hit karein — real concurrency! 🚦
					 */
					startLatch.await();

					// Actual addMoney call — real DB hit hoga
					AddMoneyRequest req = new AddMoneyRequest();
					req.setUserId(userId);
					req.setAmount(amountPerThread);

					walletService.addMoney(req);

					/**
					 * incrementAndGet() — thread-safe +1
					 * Agar addMoney successfully complete hua → success count badha do
					 */
					successCount.incrementAndGet();

				} catch (Exception e) {
					/**
					 * addMoney fail hua toh yahan aayenge
					 * addMoney mein exception nahi aana chahiye — isliye error print karo
					 */
					System.err.println("addMoney failed: " + e.getMessage());
					failCount.incrementAndGet();

				} finally {
					/**
					 * finally — success ho ya fail, HAMESHA chalega
					 *
					 * doneLatch.countDown() — "mera kaam khatam"
					 * 10 threads → 10 baar countDown → doneLatch 0 pe aayega
					 * Tab main thread aage badhegi
					 *
					 * finally mein isliye rakha — agar exception bhi aaye
					 * toh bhi doneLatch.countDown() hoga
					 * Warna main thread forever wait karti reh jaati!
					 */
					doneLatch.countDown();
				}
			});
		}

		// =====================================================
		// STEP 6 — FIRE!
		// =====================================================

		/**
		 * startLatch.countDown() — PISTOL FIRE! 🔫
		 *
		 * Abhi tak sab threads await() pe ruki hain
		 * Yeh line chalte hi — sab ek saath chhoot jaayengi
		 * Real concurrent requests simulate hogi
		 */
		startLatch.countDown();

		/**
		 * doneLatch.await(30, TimeUnit.SECONDS)
		 *
		 * Main thread yahan rukegi — jab tak sab 10 threads done na ho jaayein
		 * 30 seconds timeout — agar 30s mein sab complete nahi hua toh aage badho
		 * (stuck thread ke wajah se test forever na ruke)
		 */
		doneLatch.await(30, TimeUnit.SECONDS);

		/**
		 * executor.shutdown() — office band karo 🏢
		 * Sab threads ka kaam khatam — pool destroy karo
		 * Memory free karo
		 */
		executor.shutdown();

		// =====================================================
		// STEP 7 — RESULT CHECK KARO
		// =====================================================

		// DB se actual balance fetch karo
		BigDecimal finalBalance = walletService.getBalance(userId);

		/**
		 * Expected balance = amount × successful threads
		 *
		 * Agar sab 10 success → 100 × 10 = ₹1000
		 * Agar kuch fail bhi hue → 100 × successCount
		 *
		 * successCount se multiply isliye — fail threads ne paisa add nahi kiya
		 */
		BigDecimal expectedBalance = amountPerThread.multiply(new BigDecimal(successCount.get()));

		System.out.println("=== ADD MONEY CONCURRENCY TEST ===");
		System.out.println("Threads: " + threadCount);
		System.out.println("Success: " + successCount.get());
		System.out.println("Failed:  " + failCount.get());
		System.out.println("Expected Balance: " + expectedBalance);
		System.out.println("Actual Balance:   " + finalBalance);

		/**
		 * FINAL ASSERTION — sabse important line
		 *
		 * assertThat(finalBalance).isEqualByComparingTo(expectedBalance)
		 *
		 * isEqualByComparingTo — BigDecimal ke liye use karo
		 * .equals() mat use karna — 1000 aur 1000.0 ko equals() different maanta hai!
		 * isEqualByComparingTo — sirf value compare karta hai, scale ignore karta hai
		 *
		 * Agar finalBalance != expectedBalance:
		 *   → Race condition hai → FOR UPDATE lock kaam nahi kar raha ❌
		 *
		 * Agar finalBalance == expectedBalance:
		 *   → Locking sahi kaam kar raha hai ✅
		 */
		assertThat(finalBalance).isEqualByComparingTo(expectedBalance);
		System.out.println("✅ No race condition! Balance is correct.");
	}

}
