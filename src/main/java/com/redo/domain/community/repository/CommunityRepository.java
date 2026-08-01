package com.redo.domain.community.repository;

import com.redo.domain.community.entity.Community;
import com.redo.domain.community.enums.CommunityCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {

    Page<Community> findByDeletedAtIsNull(Pageable pageable);

    Page<Community> findByCategoryAndDeletedAtIsNull(CommunityCategory category, Pageable pageable);

    Optional<Community> findByIdAndDeletedAtIsNull(Long id);

    // 목록 조회용: 게시글별 작성자 닉네임을 단건 쿼리 반복 없이 한 번에 조회한다.
    // 프로필이 없는 사용자도 게시글은 조회되어야 하므로 UserProfile 은 LEFT JOIN 한다.
    @Query("""
            SELECT c.id AS communityId, p.nickname AS nickname
            FROM Community c
            LEFT JOIN UserProfile p ON p.user = c.user
            WHERE c IN :communities
            """)
    List<CommunityWriter> findWritersByCommunities(@Param("communities") List<Community> communities);

    // 게시글별 작성자 닉네임 조회 결과 projection
    interface CommunityWriter {
        Long getCommunityId();
        String getNickname();
    }

    // 동시 요청에서 갱신 유실(Lost Update)이 발생하지 않도록 좋아요 수를 DB에서 원자적으로 증가시킨다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Community c SET c.likeCount = COALESCE(c.likeCount, 0) + 1 WHERE c.id = :communityId")
    void increaseLikeCount(@Param("communityId") Long communityId);

    // 좋아요 수를 DB에서 원자적으로 감소시킨다(0 미만으로 내려가지 않도록 보정).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Community c
            SET c.likeCount = CASE WHEN COALESCE(c.likeCount, 0) > 0 THEN c.likeCount - 1 ELSE 0 END
            WHERE c.id = :communityId
            """)
    void decreaseLikeCount(@Param("communityId") Long communityId);

    // 원자적 증감 직후 최신 좋아요 수를 응답에 담기 위한 조회
    @Query("SELECT c.likeCount FROM Community c WHERE c.id = :communityId")
    Integer findLikeCountById(@Param("communityId") Long communityId);
}
