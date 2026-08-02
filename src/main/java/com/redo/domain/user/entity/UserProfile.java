package com.redo.domain.user.entity;

import com.redo.domain.user.enums.Gender;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "nickname", length = 30, nullable = false, unique = true)
    private String nickname;

    @Column(name = "profile_image_key", length = 500)
    private String profileImageKey;

    @Column(name = "character_code", nullable = false, length = 50)
    private String characterCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender; // FEMALE, MALE, NONE, UNKNOWN

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "intro_message", length = 100)
    private String introMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public static UserProfile create(User user, String nickname, String characterCode, Gender gender, LocalDate birthDate) {
        UserProfile profile = new UserProfile();
        profile.user = user;
        profile.nickname = nickname;
        profile.characterCode = characterCode;
        profile.gender = gender;
        profile.birthDate = birthDate;
        return profile;
    }

    public void updateProfileImageKey(String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }

    public void updateCharacterCode(String characterCode) {
        this.characterCode = characterCode;
    }
}

