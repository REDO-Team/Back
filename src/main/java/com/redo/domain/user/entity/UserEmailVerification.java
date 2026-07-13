package com.redo.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_email_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserEmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // userId는 null일수있음.
    private User user;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt; // 인증이 완료되기 전에는 이 값이 비어있어야 하니까 null가능

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verification_code", length = 6)
    private String verificationCode; // 인증번호 저장할 필드 추이

    public static UserEmailVerification create(String email, String verificationCode) {
        UserEmailVerification verification = new UserEmailVerification();
        verification.email = email;
        verification.verificationCode = verificationCode;
        return verification;
    }

    //인증번호 확인 api에서 verifiedAt을 업데이트하는 메서드
    public void verify() {
        this.verifiedAt = LocalDateTime.now();
    }
}