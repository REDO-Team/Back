package com.redo.domain.point.repository;

import com.redo.domain.point.entity.PointTransaction;
import com.redo.domain.point.enums.PointTransactionType;
import com.redo.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    long countByUserAndTransactionTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            User user,
            PointTransactionType transactionType,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    // 이번 달 적립 합계 쿼리
    @Query("""
        select coalesce(sum(p.amount), 0)
        from PointTransaction p
        where p.user = :user
          and p.transactionType = :transactionType
          and p.createdAt >= :startAt
          and p.createdAt < :endAt
        """)
    Long sumAmountByUserAndTransactionTypeAndCreatedAtBetween(
            @Param("user") User user,
            @Param("transactionType") PointTransactionType transactionType,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    // 포인트 거래 내역 커서 조회 쿼리
    @Query("""
            select p
            from PointTransaction p
            left join fetch p.rewardRedemption rr
            left join fetch rr.rewardProduct
            where p.user = :user
              and (:cursor is null or p.id < :cursor)
            order by p.id desc
            """)
    List<PointTransaction> findByUserWithRewardRedemptionAndProduct(
            @Param("user") User user,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
