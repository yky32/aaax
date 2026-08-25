package com.aaax.server;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AppTests {

	@Test
	@Disabled("Requires Kafka/Redis/PostgreSQL — run locally with full infrastructure only")
	void contextLoads() {
	}

}
