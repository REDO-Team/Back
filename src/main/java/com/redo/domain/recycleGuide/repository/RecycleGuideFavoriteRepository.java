package com.redo.domain.recycleGuide.repository;

import com.redo.domain.recycleGuide.entity.Mapping.RecycleGuideFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecycleGuideFavoriteRepository extends JpaRepository<RecycleGuideFavorite, Long> {

    boolean existsByUserIdAndRecycleGuideId(Long userId, Long recycleGuideId);

    @Query("SELECT f FROM RecycleGuideFavorite f " +
           "JOIN FETCH f.recycleGuide " +
           "WHERE f.user.id = :userId " +
           "ORDER BY f.createdAt DESC")
    List<RecycleGuideFavorite> findAllByUserId(@Param("userId") Long userId);
}
