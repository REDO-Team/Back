package com.redo.domain.community.repository;

import com.redo.domain.community.entity.Community;
import com.redo.domain.community.entity.CommunityComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    // 목록 조회용: 게시글별 삭제되지 않은 댓글 수를 단건 쿼리 반복 없이 한 번에 집계한다.
    @Query("""
            SELECT c.community.id AS communityId, COUNT(c) AS commentCount
            FROM CommunityComment c
            WHERE c.community IN :communities AND c.deletedAt IS NULL
            GROUP BY c.community.id
            """)
    List<CommunityCommentCount> findCommentCountsByCommunities(@Param("communities") List<Community> communities);

    // 게시글별 댓글 수 집계 결과 projection
    interface CommunityCommentCount {
        Long getCommunityId();
        long getCommentCount();
    }

    // 상세 조회용: 단일 게시글의 삭제되지 않은 댓글 수를 집계한다.
    long countByCommunityAndDeletedAtIsNull(Community community);

    List<CommunityComment> findByCommunityAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
            Community community,
            Long cursor,
            Pageable pageable
    );

    Optional<CommunityComment> findByIdAndDeletedAtIsNull(Long id);

    // 내가 작성한 댓글 목록 조회용
    // 원본 게시글이 삭제된 댓글은 이동할 대상이 없으므로 목록에서 제외한다.
    // 게시글 정보를 함께 노출하므로 community 는 fetch join 으로 한 번에 가져온다.
    @Query(value = """
            SELECT c
            FROM CommunityComment c
            JOIN FETCH c.community community
            WHERE c.user.id = :userId
              AND c.deletedAt IS NULL
              AND community.deletedAt IS NULL
            """,
            countQuery = """
                    SELECT COUNT(c)
                    FROM CommunityComment c
                    WHERE c.user.id = :userId
                      AND c.deletedAt IS NULL
                      AND c.community.deletedAt IS NULL
                    """)
    Page<CommunityComment> findMyComments(@Param("userId") Long userId, Pageable pageable);
}
