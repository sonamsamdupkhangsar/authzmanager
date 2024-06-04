package me.sonam.authzmanager;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

//@ActiveProfiles("local")
//@SpringBootTest
class ApplicationTests {
	private static final Logger LOG = LoggerFactory.getLogger(ApplicationTests.class);

	@Test
	void contextLoads() {
	}

	@Test
	public void instant() {
		final String clientIdIssuedAt = "2024-05-27T23:29";

		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm");
		try {
			Date date = formatter.parse(clientIdIssuedAt);
			LOG.info("date {} in instant: {}", date, date.toInstant());
		} catch (ParseException e) {
			LOG.error("failed to parse clientIdIssuedAt to dateformat", e);
		}
	}

	@Test
	public void e9number() {
		final String clientIdIssuedAt = "2024-05-28T13:47:00Z";
		final String num = "1.71690402E9";

		long value = Double.valueOf(num).longValue();
		LOG.info("{} in long: {}", num, value);

		Instant instant = Instant.ofEpochSecond(value);
		LOG.info("instant value is {} for long: {}", instant, value);


		Map<String, Object> map = new HashMap<>();
		map.put("clientIdIssuedAt", clientIdIssuedAt);

		LOG.info("clientIdIssuedAt: {}", map.get("clientIdIssuedAt"));
	}

	@Test
	public void testInstant() {
		Instant clientIdIssuedAt = Instant.parse("2024-05-28T00:39:00Z");
		LOG.info("clientIdIssuedAt: {}", clientIdIssuedAt);


		LocalDateTime localDateTime = LocalDateTime.ofInstant(clientIdIssuedAt, ZoneId.systemDefault());

		Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
		LOG.info("instant: {}, localDateTime: {}", instant, localDateTime);
	}

}
