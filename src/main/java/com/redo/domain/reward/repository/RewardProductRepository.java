package com.redo.domain.reward.repository;

import com.redo.domain.reward.entity.RewardProduct;
import com.redo.domain.reward.enums.RewardProductStatus;
import com.redo.domain.reward.enums.RewardProductType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RewardProductRepository extends JpaRepository<RewardProduct, Long> {

    Page<RewardProduct> findByStatusAndStockQuantityGreaterThan(
            RewardProductStatus status,
            Integer stockQuantity,
            Pageable pageable
    );

    Page<RewardProduct> findByRewardProductTypeAndStatusAndStockQuantityGreaterThan(
            RewardProductType rewardProductType,
            RewardProductStatus status,
            Integer stockQuantity,
            Pageable pageable
    );

    Optional<RewardProduct> findByIdAndStatusAndStockQuantityGreaterThan(
            Long id,
            RewardProductStatus status,
            Integer stockQuantity
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rp from RewardProduct rp where rp.id = :rewardProductId")
    Optional<RewardProduct> findByIdForUpdate(@Param("rewardProductId") Long rewardProductId);
}
