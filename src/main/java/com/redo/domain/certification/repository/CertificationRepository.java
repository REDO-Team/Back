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

    // 데모데이 시현을 위해 동일 품목 일일 중복 제한을 비활성화함 (2026-08-21)
    // 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
    // boolean existsByUserIdAndRecycleGuideIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
    //         Long userId,
    //         Long recycleGuideId,
    //         CertificationStatus status,
    //         LocalDateTime startAt,
    //         LocalDateTime endAt
    // );

    Optional<Certification> findTopByUserIdAndStatusInOrderByJudgedAtDesc(
            Long userId,
            Collection<CertificationStatus> statuses
    );

    // 데모데이 시현을 위해 일일 3회/5분 제한을 비활성화함 (2026-08-20)
    // 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
    // Optional<Certification> findTopByUserIdAndStatusAndJudgedAtIsNotNullOrderByJudgedAtDesc(
    //         Long userId,
    //         CertificationStatus status
    // );
}
