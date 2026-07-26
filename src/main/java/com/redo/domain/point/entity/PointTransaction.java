package com.redo.domain.point.entity;

import com.redo.domain.certification.entity.Certification;
import com.redo.domain.point.enums.PointTransactionType;
import com.redo.domain.reward.entity.RewardRedemption;
import com.redo.domain.user.entity.User;
import com.redo.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "point_transactions",
        indexes = {
                @Index(
                        name = "idx_point_transactions_user_type_created_at",
                        columnList = "user_id, transaction_type, created_at"
                ),
                @Index(
                        name = "idx_point_transactions_user_id",
                        columnList = "user_id, id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certification_id")
    private Certification certification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_redemption_id")
    private RewardRedemption rewardRedemption;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private PointTransactionType transactionType;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
    private String idempotencyKey;
}
