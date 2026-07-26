package com.redo.domain.reward.entity;

import com.redo.domain.user.entity.User;
import com.redo.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "reward_redemptions",
        indexes = {
                @Index(
                        name = "idx_reward_redemptions_user_created_at",
                        columnList = "user_id, created_at"
                ),
                @Index(
                        name = "idx_reward_redemptions_user_id",
                        columnList = "user_id, id"
                )
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reward_redemptions_user_idempotency_key",
                columnNames = {"user_id", "idempotency_key"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RewardRedemption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_product_id", nullable = false)
    private RewardProduct rewardProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_id")
    private ShippingAddress shippingAddress;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "product_image_key", nullable = false, length = 500)
    private String productImageKey;

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 30)
    private String receiverPhone;

    @Column(name = "postal_code", length = 5)
    private String postalCode;

    @Column(name = "address1", length = 255)
    private String address1;

    @Column(name = "address2", length = 255)
    private String address2;

    @Column(name = "used_point", nullable = false)
    private Integer usedPoint;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;
}
