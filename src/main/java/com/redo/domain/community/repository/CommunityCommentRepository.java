package com.redo.domain.community.repository;

import com.redo.domain.community.entity.Community;
import com.redo.domain.community.entity.CommunityComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    long countByCommunityAndDeletedAtIsNull(Community community);

    List<CommunityComment> findByCommunityAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
            Community community,
            Long cursor,
            Pageable pageable
    );

    Optional<CommunityComment> findByIdAndDeletedAtIsNull(Long id);
}
