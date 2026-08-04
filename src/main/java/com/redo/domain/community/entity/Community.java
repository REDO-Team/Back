package com.redo.domain.community.entity;

import com.redo.domain.community.enums.CommunityCategory;
import com.redo.domain.user.entity.User;
import com.redo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "community")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Community extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommunityCategory category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "like_count")
    private Integer likeCount;

    // 게시글 수정: 작성자만 호출하며 카테고리/제목/본문을 갱신한다(첨부 이미지는 별도로 교체한다).
    public void update(CommunityCategory category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
    }

    // 첨부 이미지만 바뀐 경우처럼 게시글 컬럼 자체는 그대로일 때도 수정 시각을 갱신하기 위해 호출한다.
    // (updatedAt 이 바뀌면서 엔티티가 변경 상태가 되어 UPDATE 가 나가고, BaseEntity 의 @PreUpdate 도 동작한다.)
    public void touch() {
        onUpdate();
    }

    // 소프트 삭제: 실제 삭제 대신 deletedAt 을 기록해 조회에서 제외한다.
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

}
