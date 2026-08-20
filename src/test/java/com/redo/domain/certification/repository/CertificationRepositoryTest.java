package com.redo.domain.certification.repository;

import com.redo.TestcontainersConfiguration;
import com.redo.domain.certification.enums.CertificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CertificationRepositoryTest {

    @Autowired
    private CertificationRepository certificationRepository;

    @Test
    void policyQueriesAreCreatedAndReturnEmptyResultsWithoutCertifications() {
        LocalDateTime startAt = LocalDateTime.of(2026, 7, 23, 0, 0);
        LocalDateTime endAt = startAt.plusDays(1);

        assertThat(certificationRepository
                .countByUserIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
                        999L,
                        CertificationStatus.PASSED,
                        startAt,
                        endAt
                )).isZero();
        assertThat(certificationRepository
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(
                        999L,
                        CertificationStatus.PROCESSING
                )).isEmpty();
        // 데모데이 시현을 위해 일일 3회/5분 제한을 비활성화함 (2026-08-20)
        // 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
        // assertThat(certificationRepository
        //         .findTopByUserIdAndStatusAndJudgedAtIsNotNullOrderByJudgedAtDesc(
        //                 999L,
        //                 CertificationStatus.PASSED
        //         )).isEmpty();
    }
}
