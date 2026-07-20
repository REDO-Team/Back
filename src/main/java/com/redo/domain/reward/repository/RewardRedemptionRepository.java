package com.redo.domain.reward.repository;

import com.redo.domain.reward.entity.RewardRedemption;
import com.redo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, Long> {

    boolean existsByUserAndIdempotencyKey(User user, String idempotencyKey);
}
