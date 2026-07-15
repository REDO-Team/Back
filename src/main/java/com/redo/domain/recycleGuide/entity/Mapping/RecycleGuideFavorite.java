package com.redo.domain.recycleGuide.entity.Mapping;



import com.redo.domain.recycleGuide.entity.RecycleGuide;
import com.redo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecycleGuideFavorite {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    private RecycleGuide recycleGuide;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
