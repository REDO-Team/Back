package com.redo;

import com.redo.domain.certification.repository.AiJudgementRepository;
import com.redo.domain.certification.repository.CertificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RedoApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CertificationRepository certificationRepository;

	@Autowired
	private AiJudgementRepository aiJudgementRepository;

	@Test
	void contextLoads() {
		Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

		assertThat(result).isEqualTo(1);
		assertThat(certificationRepository).isNotNull();
		assertThat(aiJudgementRepository).isNotNull();
	}

}
