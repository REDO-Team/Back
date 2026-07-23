package com.redo.domain.certification.repository;

import com.redo.domain.certification.entity.Certification;
import com.redo.domain.certification.enums.CertificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    Optional<Certification> findByIdAndUserId(Long certificationId, Long userId);

    boolean existsByUserIdAndStatus(Long userId, CertificationStatus status);

    long countByUserIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
            Long userId,
            CertificationStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    boolean existsByUserIdAndRecycleGuideIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
            Long userId,
            Long recycleGuideId,
            CertificationStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    Optional<Certification> findTopByUserIdAndStatusInOrderByJudgedAtDesc(
            Long userId,
            Collection<CertificationStatus> statuses
    );
}
