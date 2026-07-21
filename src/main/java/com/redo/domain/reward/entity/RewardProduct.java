package com.redo.domain.reward.entity;

import com.redo.domain.reward.enums.RewardProductStatus;
import com.redo.domain.reward.enums.RewardProductType;
import com.redo.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reward_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RewardProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_product_type", nullable = false, length = 30)
    private RewardProductType rewardProductType;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "usage_guide", columnDefinition = "TEXT")
    private String usageGuide;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "image_key", nullable = false, length = 500)
    private String imageKey;

    @Column(name = "price_point", nullable = false)
    private Integer pricePoint;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RewardProductStatus status;

    // 리워드 상품 구매 시 재고를 1개 차감하는 메서드
    public void decreaseStock() {
        if (this.stockQuantity == null || this.stockQuantity <= 0) {
            throw new IllegalStateException("상품 재고가 부족합니다.");
        }

        this.stockQuantity -= 1;

        if (this.stockQuantity == 0) {
            this.status = RewardProductStatus.SOLD_OUT;
        }
    }
}
