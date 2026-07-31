package com.redo.domain.certification.repository;

import com.redo.domain.certification.entity.Certification;
import com.redo.domain.certification.enums.CertificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    Optional<Certification> findByIdAndUserId(Long certificationId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from Certification c
            where c.id = :certificationId
              and c.user.id = :userId
            """)
    Optional<Certification> findByIdAndUserIdForUpdate(
            @Param("certificationId") Long certificationId,
            @Param("userId") Long userId
    );

    boolean existsByUserIdAndStatus(Long userId, CertificationStatus status);

    long countByUserIdAndStatus(Long userId, CertificationStatus status);

    long countByUserIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
            Long userId,
            CertificationStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    Optional<Certification> findTopByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            CertificationStatus status
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

    Optional<Certification> findTopByUserIdAndStatusInAndJudgedAtIsNotNullOrderByJudgedAtDesc(
            Long userId,
            Collection<CertificationStatus> statuses
    );
}
