package me.sonam.authzmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@SpringBootTest
class ApplicationTests {
	@MockBean
	private ReactiveJwtDecoder reactiveJwtDecoder;
	@Test
	void contextLoads() {
	}

}
