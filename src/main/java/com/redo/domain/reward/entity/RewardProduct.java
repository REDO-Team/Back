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

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "price_point", nullable = false)
    private Integer pricePoint;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RewardProductStatus status;
}
