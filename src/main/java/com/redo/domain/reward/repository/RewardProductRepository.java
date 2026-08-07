package com.redo.domain.reward.repository;

import com.redo.domain.reward.entity.RewardProduct;
import com.redo.domain.reward.enums.RewardProductStatus;
import com.redo.domain.reward.enums.RewardProductType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RewardProductRepository extends JpaRepository<RewardProduct, Long> {

    @Query("""
            select rp
            from RewardProduct rp
            where rp.status = :status
              and rp.stockQuantity > :stockQuantity
              and (:rewardProductType is null or rp.rewardProductType = :rewardProductType)
              and (
                  :cursor is null
                  or rp.pricePoint > (
                      select cursorProduct.pricePoint
                      from RewardProduct cursorProduct
                      where cursorProduct.id = :cursor
                  )
                  or (
                      rp.pricePoint = (
                          select cursorProduct.pricePoint
                          from RewardProduct cursorProduct
                          where cursorProduct.id = :cursor
                      )
                      and rp.id > :cursor
                  )
              )
            order by rp.pricePoint asc, rp.id asc
            """)
    List<RewardProduct> findAvailableProducts(
            @Param("rewardProductType") RewardProductType rewardProductType,
            @Param("status") RewardProductStatus status,
            @Param("stockQuantity") Integer stockQuantity,
            @Param("cursor") Long cursor,
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

    @Query("""
            select rp
            from RewardProduct rp
            where rp.status = :status
              and rp.stockQuantity > :stockQuantity
            order by function('RAND', :seed)
            """)
    List<RewardProduct> findRandomAvailableProducts(
            @Param("status") RewardProductStatus status,
            @Param("stockQuantity") Integer stockQuantity,
            @Param("seed") long seed,
            Pageable pageable
    );

    @Query("""
            select rp
            from RewardProduct rp
            where rp.status = :status
              and rp.stockQuantity > :stockQuantity
              and rp.id not in :excludedIds
            order by function('RAND', :seed)
            """)
    List<RewardProduct> findRandomAvailableProductsExcludingIds(
            @Param("status") RewardProductStatus status,
            @Param("stockQuantity") Integer stockQuantity,
            @Param("excludedIds") List<Long> excludedIds,
            @Param("seed") long seed,
            Pageable pageable
    );

    @Query("""
            select rp
            from RewardProduct rp
            where rp.id in :rewardProductIds
              and rp.status = :status
              and rp.stockQuantity > :stockQuantity
            """)
    List<RewardProduct> findAvailableProductsByIdIn(
            @Param("rewardProductIds") List<Long> rewardProductIds,
            @Param("status") RewardProductStatus status,
            @Param("stockQuantity") Integer stockQuantity
    );
}
