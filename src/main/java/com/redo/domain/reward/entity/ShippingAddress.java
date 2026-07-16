package com.redo.domain.reward.entity;

import com.redo.domain.reward.enums.ShippingAddressType;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    private ShippingAddressType addressType;

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(name = "postal_code", nullable = false, length = 5)
    private String postalCode;

    @Column(nullable = false, length = 255)
    private String address1;

    @Column(length = 255)
    private String address2;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void update(
            ShippingAddressType addressType,
            String receiverName,
            String phone,
            String postalCode,
            String address1,
            String address2,
            Boolean isDefault
    ) {
        this.addressType = addressType;
        this.receiverName = receiverName;
        this.phone = phone;
        this.postalCode = postalCode;
        this.address1 = address1;
        this.address2 = address2;
        this.isDefault = isDefault;
    }

    public void setDefault() {
        this.isDefault = true;
    }

    public void unsetDefault() {
        this.isDefault = false;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.isDefault = false;
    }
}
