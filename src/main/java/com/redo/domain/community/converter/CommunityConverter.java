package com.redo.domain.community.converter;

import com.redo.domain.community.dto.res.CommunityCommentCreateResponseDTO;
import com.redo.domain.community.dto.res.CommunityCommentDeleteResponseDTO;
import com.redo.domain.community.dto.res.CommunityCommentListResponseDTO;
import com.redo.domain.community.dto.res.CommunityCommentResponseDTO;
import com.redo.domain.community.dto.res.CommunityCreateResponseDTO;
import com.redo.domain.community.dto.res.CommunityDeleteResponseDTO;
import com.redo.domain.community.dto.res.CommunityDetailResponseDTO;
import com.redo.domain.community.dto.res.CommunityLikeResponseDTO;
import com.redo.domain.community.dto.res.CommunityPageResponseDTO;
import com.redo.domain.community.dto.res.CommunityResponseDTO;
import com.redo.domain.community.entity.Community;
import com.redo.domain.community.entity.CommunityComment;
import com.redo.domain.community.entity.CommunityImg;
import com.redo.domain.community.entity.CommunityLike;
import com.redo.domain.community.entity.CommunityLikeId;
import com.redo.domain.community.enums.CommunityCategory;
import com.redo.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

public class CommunityConverter {

    // 신규 게시글의 기본 상태값. status 컬럼은 NOT NULL 이지만 별도 enum/용도가 정의되지 않아 기본값으로 저장한다.
    private static final String DEFAULT_STATUS = "ACTIVE";

    private CommunityConverter() {
    }

    public static CommunityResponseDTO toCommunityResponse(Community community, long numComments) {
        return new CommunityResponseDTO(
                community.getId(),
                String.valueOf(community.getCategory().getCode()),
                community.getTitle(),
                // TODO: CommunityImg(imageKey) -> 이미지 URL 변환. S3 인프라 구축 후 대표 이미지 URL 매핑.
                null,
                numComments,
                community.getCreatedAt()
        );
    }

    public static CommunityDetailResponseDTO toCommunityDetailResponse(Community community, String writer) {
        return new CommunityDetailResponseDTO(
                community.getId(),
                community.getTitle(),
                writer,
                community.getContent(),
                community.getCreatedAt(),
                // TODO: CommunityImg(imageKey) -> 이미지 URL 변환. S3 인프라 구축 후 대표 이미지 URL 매핑.
                null
        );
    }

    public static CommunityPageResponseDTO toCommunityPageResponse(Page<CommunityResponseDTO> page) {
        return new CommunityPageResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.hasNext()
        );
    }

    public static Community toCommunity(User user, CommunityCategory category, String title, String content) {
        return Community.builder()
                .user(user)
                .category(category)
                .title(title)
                .content(content)
                .status(DEFAULT_STATUS)
                .likeCount(0)
                .build();
    }

    public static CommunityImg toCommunityImg(Community community, String imageKey, int displayOrder) {
        return CommunityImg.builder()
                .community(community)
                .imageKey(imageKey)
                .displayOrder(displayOrder)
                .build();
    }

    public static CommunityCreateResponseDTO toCommunityCreateResponse(
            Community community,
            List<String> imageUrls,
            String writer
    ) {
        return new CommunityCreateResponseDTO(
                community.getId(),
                community.getTitle(),
                community.getContent(),
                imageUrls,
                writer,
                community.getCreatedAt()
        );
    }

    public static CommunityDeleteResponseDTO toCommunityDeleteResponse(Long communityId) {
        return new CommunityDeleteResponseDTO(communityId);
    }

    public static CommunityComment toCommunityComment(Community community, User user, String content) {
        return CommunityComment.builder()
                .community(community)
                .user(user)
                .content(content)
                .status(DEFAULT_STATUS)
                .build();
    }

    public static CommunityCommentResponseDTO toCommunityCommentResponse(CommunityComment comment, String writer) {
        return new CommunityCommentResponseDTO(
                comment.getId(),
                writer,
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    public static CommunityCommentListResponseDTO toCommunityCommentListResponse(
            List<CommunityCommentResponseDTO> comments
    ) {
        return new CommunityCommentListResponseDTO(comments);
    }

    public static CommunityCommentCreateResponseDTO toCommunityCommentCreateResponse(Long commentId) {
        return new CommunityCommentCreateResponseDTO(commentId);
    }

    public static CommunityCommentDeleteResponseDTO toCommunityCommentDeleteResponse(Long commentId) {
        return new CommunityCommentDeleteResponseDTO(commentId);
    }

    public static CommunityLike toCommunityLike(Community community, User user) {
        return CommunityLike.builder()
                .id(new CommunityLikeId(community.getId(), user.getId()))
                .community(community)
                .user(user)
                .build();
    }

    public static CommunityLikeResponseDTO toCommunityLikeResponse(Long userId, Integer likeCount) {
        return new CommunityLikeResponseDTO(userId, likeCount);
    }
}
