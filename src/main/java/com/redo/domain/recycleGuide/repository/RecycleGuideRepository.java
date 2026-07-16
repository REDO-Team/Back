package com.redo.domain.recycleGuide.repository;

import com.redo.domain.recycleGuide.entity.RecycleGuide;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecycleGuideRepository extends JpaRepository<RecycleGuide, Long> {
}
