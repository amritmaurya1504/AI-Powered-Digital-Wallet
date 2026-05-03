package com.digital.wallet;

import com.digital.wallet.dtos.AddMoneyRequest;
import com.digital.wallet.services.WalletService;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

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
	private RedisTemplate<String, String> redisTemplate;

	@PostConstruct
	@Test
	public void testRedisConnection() {
		try {
			redisTemplate.opsForValue().set("testKey", "helloRedis");
			String value = redisTemplate.opsForValue().get("testKey");

			System.out.println("Redis working: " + value);
		} catch (Exception e) {
			System.out.println("Redis NOT connected ❌");
			e.printStackTrace();
		}
	}

}
