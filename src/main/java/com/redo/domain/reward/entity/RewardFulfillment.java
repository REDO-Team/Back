package com.redo.domain.reward.entity;

import com.redo.domain.reward.enums.RewardFulfillmentStatus;
import com.redo.domain.reward.enums.RewardFulfillmentType;
import com.redo.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reward_fulfillments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RewardFulfillment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_redemption_id", nullable = false, unique = true)
    private RewardRedemption rewardRedemption;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_fulfillment_type", nullable = false, length = 30)
    private RewardFulfillmentType rewardFulfillmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RewardFulfillmentStatus status;

    @Column(name = "courier_name", length = 50)
    private String courierName;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "coupon_code_encrypted", length = 500)
    private String couponCodeEncrypted;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
