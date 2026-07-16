package com.redo.domain.recycleGuide.repository;

import com.redo.domain.recycleGuide.entity.Mapping.RecycleGuideFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecycleGuideFavoriteRepository extends JpaRepository<RecycleGuideFavorite, Long> {

    boolean existsByUserIdAndRecycleGuideId(Long userId, Long recycleGuideId);
}
