package com.redo.domain.contribution.entity;

import com.redo.domain.certification.entity.Certification;
import com.redo.domain.contribution.enums.ContributionMilestone;
import com.redo.domain.contribution.enums.ContributionType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "contribution_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contribution_events_certification",
                columnNames = "certification_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContributionEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certification_id", nullable = false)
    private Certification certification;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private ContributionType eventType;

    @Column(name = "certification_count", nullable = false)
    private Long certificationCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_milestone", length = 30)
    private ContributionMilestone targetMilestone;

    @Column(name = "remaining_count")
    private Integer remainingCount;

    @Column(name = "show_remaining_count", nullable = false)
    private boolean showRemainingCount;

    public static ContributionEvent create(
            User user,
            Certification certification,
            ContributionType eventType,
            long certificationCount,
            ContributionMilestone targetMilestone,
            Integer remainingCount,
            boolean showRemainingCount
    ) {
        ContributionEvent event = new ContributionEvent();
        event.user = user;
        event.certification = certification;
        event.eventType = eventType;
        event.certificationCount = certificationCount;
        event.targetMilestone = targetMilestone;
        event.remainingCount = remainingCount;
        event.showRemainingCount = showRemainingCount;
        return event;
    }
}
