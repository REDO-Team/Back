package com.redo.domain.community.converter;

import com.redo.domain.community.dto.res.CommunityCommentCreateResponseDTO;
import com.redo.domain.community.dto.res.CommunityCommentDeleteResponseDTO;
import com.redo.domain.community.dto.res.CommunityCommentListResponseDTO;
import com.redo.domain.community.dto.res.CommunityCommentResponseDTO;
import com.redo.domain.community.dto.res.CommunityCommentUpdateResponseDTO;
import com.redo.domain.community.dto.res.CommunityCreateResponseDTO;
import com.redo.domain.community.dto.res.CommunityDeleteResponseDTO;
import com.redo.domain.community.dto.res.CommunityDetailResponseDTO;
import com.redo.domain.community.dto.res.CommunityImageResponseDTO;
import com.redo.domain.community.dto.res.CommunityLikeResponseDTO;
import com.redo.domain.community.dto.res.CommunityPageResponseDTO;
import com.redo.domain.community.dto.res.CommunityResponseDTO;
import com.redo.domain.community.dto.res.CommunityUpdateResponseDTO;
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

    // 목록의 본문 미리보기 노출 길이. 이 길이를 넘으면 잘라내고 말줄임표를 붙인다.
    private static final int PREVIEW_LENGTH = 20;
    private static final String PREVIEW_ELLIPSIS = "...";

    private CommunityConverter() {
    }

    public static CommunityResponseDTO toCommunityResponse(
            Community community,
            long numComments,
            String imageUrl,
            String writer,
            String profileImageUrl,
            String characterCode
    ) {
        return new CommunityResponseDTO(
                community.getId(),
                numComments,
                community.getLikeCount() == null ? 0 : community.getLikeCount(),
                community.getCreatedAt(),
                imageUrl,
                community.getTitle(),
                String.valueOf(community.getCategory().getCode()),
                toPreview(community.getContent()),
                writer,
                profileImageUrl,
                characterCode
        );
    }

    // 본문을 목록용 미리보기로 변환하는 로직
    private static String toPreview(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= PREVIEW_LENGTH) {
            return content;
        }
        return content.substring(0, PREVIEW_LENGTH) + PREVIEW_ELLIPSIS;
    }

    public static CommunityDetailResponseDTO toCommunityDetailResponse(
            Community community,
            String writer,
            String profileImageUrl,
            String characterCode,
            List<CommunityImageResponseDTO> images,
            long numComments,
            boolean isLiked,
            boolean isMine
    ) {
        return new CommunityDetailResponseDTO(
                community.getId(),
                community.getTitle(),
                writer,
                profileImageUrl,
                characterCode,
                community.getContent(),
                community.getCreatedAt(),
                images.stream().map(CommunityImageResponseDTO::imageUrl).toList(),
                images,
                String.valueOf(community.getCategory().getCode()),
                numComments,
                community.getLikeCount() == null ? 0 : community.getLikeCount(),
                isLiked,
                isMine
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
            List<String> imageKeys,
            String writer
    ) {
        return new CommunityCreateResponseDTO(
                community.getId(),
                community.getTitle(),
                community.getContent(),
                imageKeys,
                writer,
                community.getCreatedAt()
        );
    }

    public static CommunityImageResponseDTO toCommunityImageResponse(CommunityImg image, String imageUrl) {
        return new CommunityImageResponseDTO(image.getId(), imageUrl);
    }

    // 수정 응답에는 수정 직후 화면을 바로 갱신할 수 있도록 남아 있는 이미지 전체(id + Presigned URL)를 담는다.
    public static CommunityUpdateResponseDTO toCommunityUpdateResponse(
            Community community,
            List<CommunityImageResponseDTO> images
    ) {
        return new CommunityUpdateResponseDTO(
                community.getId(),
                community.getTitle(),
                community.getContent(),
                String.valueOf(community.getCategory().getCode()),
                images,
                community.getUpdatedAt()
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

    public static CommunityCommentResponseDTO toCommunityCommentResponse(
            CommunityComment comment,
            String writer,
            String profileImageUrl,
            String characterCode,
            boolean isMine
    ) {
        return new CommunityCommentResponseDTO(
                comment.getId(),
                writer,
                profileImageUrl,
                characterCode,
                comment.getContent(),
                comment.getCreatedAt(),
                isMine
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

    public static CommunityCommentUpdateResponseDTO toCommunityCommentUpdateResponse(CommunityComment comment) {
        return new CommunityCommentUpdateResponseDTO(
                comment.getId(),
                comment.getContent(),
                comment.getUpdatedAt()
        );
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
