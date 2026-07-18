package com.redo.domain.community.repository;

import com.redo.domain.community.entity.Community;
import com.redo.domain.community.enums.CommunityCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {

    Page<Community> findByDeletedAtIsNull(Pageable pageable);

    Page<Community> findByCategoryAndDeletedAtIsNull(CommunityCategory category, Pageable pageable);

    Optional<Community> findByIdAndDeletedAtIsNull(Long id);
}
