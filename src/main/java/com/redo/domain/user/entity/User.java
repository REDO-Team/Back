package com.redo.domain.user.entity;

import com.redo.domain.user.enums.UserProvider;
import com.redo.domain.user.enums.UserRole;
import com.redo.domain.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = @Index(
                name = "idx_users_status",
                columnList = "status"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", length = 50, unique = true, nullable = false)
    private String loginId;

    @Column(name = "email", length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private UserProvider provider; // LOCAL, GOOGLE, KAKAO

    @Column(name = "provider_user_id", length = 255)
    private String providerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserStatus status; // ACTIVE, WITHDRAWN, SUSPENDED

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role; // USER, ADMIN

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    // 탈퇴시 User상태를 바꾸는 메서드
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
        this.email = "withdrawn_" + this.id + "_" + this.email;
    }

    // 포인트 적립 메서드
    public void addPoint(Integer amount) {
        this.totalPoints += amount;
    }

    // 리워드 상품 구매 시 포인트를 차감하는 메서드
    public void usePoint(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("사용 포인트는 0보다 커야 합니다.");
        }

        if (this.totalPoints == null || this.totalPoints < amount) {
            throw new IllegalStateException("보유 포인트가 부족합니다.");
        }

        this.totalPoints -= amount;
    }

    // 일반 가입 유저 객체 만드는 메서드
    public static User createGeneral(String loginId, String email, String passwordHash) {
        User user = new User();
        user.loginId = loginId;
        user.email = email;
        user.passwordHash = passwordHash;
        user.provider = UserProvider.LOCAL;
        user.status = UserStatus.ACTIVE;
        user.role = UserRole.USER;
        user.totalPoints = 0;
        return user;
    }

    // 소셜 가입 유저 객체 만드는 메서드
    public static User createSocial(UserProvider provider, String providerUserId, String email) {
        User user = new User();
        user.loginId = "social_" + UUID.randomUUID().toString().substring(0, 8);
        user.email = email;
        user.provider = provider;
        user.providerUserId = providerUserId;
        user.status = UserStatus.ACTIVE;
        user.role = UserRole.USER;
        user.totalPoints = 0;
        return user;
    }
}
