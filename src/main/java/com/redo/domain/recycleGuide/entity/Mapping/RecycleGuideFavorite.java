package com.redo.domain.recycleGuide.entity.Mapping;

import com.redo.domain.recycleGuide.entity.RecycleGuide;
import com.redo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

// 💡 1. 추가됨: 동일 유저의 동일 가이드 중복 즐겨찾기 방지 (복합 유니크 제약조건)
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_favorite_user_guide", columnNames = {"user_id", "guide_id"})
})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecycleGuideFavorite {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false) // 💡 2. 추가됨: 가이드 ID Null 방지
    private RecycleGuide recycleGuide;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)  // 💡 3. 추가됨: 유저 ID Null 방지
    private User user;
}
