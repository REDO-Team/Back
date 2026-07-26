package com.redo.domain.reward.repository;

import com.redo.domain.reward.entity.RewardFulfillment;
import com.redo.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RewardFulfillmentRepository extends JpaRepository<RewardFulfillment, Long> {

    @Query("""
            select rf
            from RewardFulfillment rf
            join fetch rf.rewardRedemption rr
            join fetch rr.rewardProduct
            where rr.user = :user
              and (:cursor is null or rr.id < :cursor)
            order by rr.id desc
            """)
    List<RewardFulfillment> findByUserWithRedemptionAndProduct(
            @Param("user") User user,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
