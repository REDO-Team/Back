package com.redo.domain.recycleGuide.repository;

import com.redo.domain.recycleGuide.entity.RecycleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecycleCategoryRepository extends JpaRepository<RecycleCategory, Long> {

    @Query("SELECT DISTINCT c FROM RecycleCategory c LEFT JOIN FETCH c.recycleGuides ORDER BY c.id")
    List<RecycleCategory> findAllWithGuides();
}
