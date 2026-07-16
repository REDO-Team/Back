package com.redo.domain.recycleGuide.entity;

import jakarta.persistence.*;
import lombok.*;


@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_step_guide_stepnum", columnNames = {"guide_id", "step_number"})
})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecycleGuideStep {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer stepNumber;

    @Column(nullable = false, length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    private RecycleGuide recycleGuide;
}