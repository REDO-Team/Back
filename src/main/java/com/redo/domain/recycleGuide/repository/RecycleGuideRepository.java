package com.redo.domain.recycleGuide.repository;

import com.redo.domain.recycleGuide.entity.RecycleGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecycleGuideRepository extends JpaRepository<RecycleGuide, Long> {

    @Query("SELECT g FROM RecycleGuide g " +
           "JOIN FETCH g.recycleCategory " +
           "LEFT JOIN FETCH g.guideSteps " +
           "WHERE g.name = :name")
    Optional<RecycleGuide> findByName(@Param("name") String name);
}
