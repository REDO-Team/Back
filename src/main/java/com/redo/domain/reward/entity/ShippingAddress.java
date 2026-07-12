package com.redo.domain.reward.entity;

import com.redo.domain.user.entity.User;
import com.redo.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ShippingAddress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 255)
    private String address1;

    @Column(length = 255)
    private String address2;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
