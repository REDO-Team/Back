package com.redo.domain.reward.repository;

import com.redo.domain.reward.entity.RewardFulfillment;
import com.redo.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardFulfillmentRepository extends JpaRepository<RewardFulfillment, Long> {

    @Query(
            value = """
                select rf
                from RewardFulfillment rf
                join fetch rf.rewardRedemption rr
                join fetch rr.rewardProduct
                where rr.user = :user
                order by rr.createdAt desc, rr.id desc
                """,
            countQuery = """
                select count(rf)
                from RewardFulfillment rf
                where rf.rewardRedemption.user = :user
                """
    )
    Page<RewardFulfillment> findPageByUserWithRedemptionAndProduct(
            @Param("user") User user,
            Pageable pageable
    );
}
