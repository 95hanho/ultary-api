package me._hanho.ultary;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "ai.moderation.worker-enabled=false")
class UltaryApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
