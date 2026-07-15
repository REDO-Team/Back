package com.redo.domain.recycleGuide.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecycleGuide {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String caution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private RecycleCategory recycleCategory;

    @OneToMany(mappedBy = "recycleGuide", cascade = CascadeType.ALL)
    @OrderBy("stepNumber ASC") // 💡 5. 추가됨: DB에서 데이터를 꺼낼 때 항상 스텝 번호 오름차순으로 정렬
    private List<RecycleGuideStep> guideSteps = new ArrayList<>();
}