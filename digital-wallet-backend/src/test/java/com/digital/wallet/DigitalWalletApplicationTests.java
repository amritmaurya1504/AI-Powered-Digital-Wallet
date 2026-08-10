package com.digital.wallet;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

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
