package com.redo.domain.community.service;

import com.redo.domain.community.converter.CommunityConverter;
import com.redo.domain.community.dto.req.CommunityCommentCreateRequestDTO;
import com.redo.domain.community.dto.req.CommunityCreateRequestDTO;
import com.redo.domain.community.dto.res.CommunityCommentCreateResponseDTO;
import com.redo.domain.community.dto.res.CommunityCommentDeleteResponseDTO;
import com.redo.domain.community.dto.res.CommunityCommentListResponseDTO;
import com.redo.domain.community.dto.res.CommunityCreateResponseDTO;
import com.redo.domain.community.dto.res.CommunityDeleteResponseDTO;
import com.redo.domain.community.dto.res.CommunityDetailResponseDTO;
import com.redo.domain.community.dto.res.CommunityLikeResponseDTO;
import com.redo.domain.community.dto.res.CommunityResponseDTO;
import com.redo.domain.community.entity.Community;
import com.redo.domain.community.entity.CommunityComment;
import com.redo.domain.community.entity.CommunityImg;
import com.redo.domain.community.entity.CommunityLike;
import com.redo.domain.community.entity.CommunityLikeId;
import com.redo.domain.community.enums.CommunityCategory;
import com.redo.domain.community.exception.CommunityException;
import com.redo.domain.community.exception.code.CommunityErrorCode;
import com.redo.domain.community.repository.CommunityCommentRepository;
import com.redo.domain.community.repository.CommunityImgRepository;
import com.redo.domain.community.repository.CommunityLikeRepository;
import com.redo.domain.community.repository.CommunityRepository;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.entity.UserProfile;
import com.redo.domain.user.exception.UserErrorCode;
import com.redo.domain.user.repository.UserProfileRepository;
import com.redo.domain.user.repository.UserRepository;
import com.redo.global.apiPayload.exception.GeneralException;
import com.redo.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    // 전체 보기 전용 카테고리 코드(특정 카테고리 필터 없이 전체 조회)
    private static final int CATEGORY_ALL = 0;

    // 게시글 이미지가 업로드되는 S3 디렉터리 접두어
    private static final String IMAGE_DIRECTORY = "community";

    private final CommunityRepository communityRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityImgRepository communityImgRepository;
    private final CommunityLikeRepository communityLikeRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final S3Service s3Service;

    // 게시글 목록 조회 로직
    public Page<CommunityResponseDTO> getCommunityPosts(Integer category, Pageable pageable) {
        Page<Community> communities = (category == null || category == CATEGORY_ALL)
                ? communityRepository.findByDeletedAtIsNull(pageable)
                : communityRepository.findByCategoryAndDeletedAtIsNull(toCategory(category), pageable);

        return communities.map(community -> CommunityConverter.toCommunityResponse(
                community,
                communityCommentRepository.countByCommunityAndDeletedAtIsNull(community),
                getRepresentativeImageUrl(community)
        ));
    }

    // 게시글 상세 조회 로직
    public CommunityDetailResponseDTO getCommunityPost(Long communityId) {
        Community community = communityRepository.findByIdAndDeletedAtIsNull(communityId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.COMMUNITY_NOT_FOUND));

        return CommunityConverter.toCommunityDetailResponse(
                community,
                getNickname(community.getUser().getId()),
                getRepresentativeImageUrl(community)
        );
    }

    // 게시글 등록 로직
    @Transactional
    public CommunityCreateResponseDTO createCommunityPost(Long userId, CommunityCreateRequestDTO request) {
        validateCreateRequest(request);
        CommunityCategory category = toCategory(request.category());
        User user = getUser(userId);

        Community community = communityRepository.save(
                CommunityConverter.toCommunity(user, category, request.title(), request.content())
        );

        List<String> imageUrls = saveImages(community, request.image()).stream()
                .map(this::createImageUrl)
                .toList();

        return CommunityConverter.toCommunityCreateResponse(community, imageUrls, getNickname(userId));
    }

    // 댓글 목록 조회 로직(comment ID 기준 커서 페이징, cursor/length 없으면 전체 반환)
    public CommunityCommentListResponseDTO getCommunityComments(Long communityId, Long cursor, Integer length) {
        Community community = getActiveCommunity(communityId);

        List<CommunityComment> comments = communityCommentRepository
                .findByCommunityAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
                        community,
                        cursor == null ? 0L : cursor,
                        length == null ? Pageable.unpaged() : PageRequest.of(0, length)
                );

        return CommunityConverter.toCommunityCommentListResponse(
                comments.stream()
                        .map(comment -> CommunityConverter.toCommunityCommentResponse(
                                comment,
                                getNickname(comment.getUser().getId())
                        ))
                        .toList()
        );
    }

    // 댓글 등록 로직
    @Transactional
    public CommunityCommentCreateResponseDTO createCommunityComment(
            Long userId,
            Long communityId,
            CommunityCommentCreateRequestDTO request
    ) {
        if (request.comment() == null || request.comment().isBlank()) {
            throw new CommunityException(CommunityErrorCode.COMMENT_REQUIRED);
        }

        Community community = getActiveCommunity(communityId);
        User user = getUser(userId);

        CommunityComment comment = communityCommentRepository.save(
                CommunityConverter.toCommunityComment(community, user, request.comment())
        );

        return CommunityConverter.toCommunityCommentCreateResponse(comment.getId());
    }

    // 댓글 삭제 로직(소프트 삭제)
    @Transactional
    public CommunityCommentDeleteResponseDTO deleteCommunityComment(Long userId, Long communityId, Long commentId) {
        Community community = getActiveCommunity(communityId);

        CommunityComment comment = communityCommentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.COMMENT_NOT_FOUND));

        // 다른 게시글에 달린 댓글 id 로 접근하는 경우도 존재하지 않는 것으로 처리한다.
        if (!comment.getCommunity().getId().equals(community.getId())) {
            throw new CommunityException(CommunityErrorCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getUser().getId().equals(userId)) {
            throw new CommunityException(CommunityErrorCode.NOT_COMMENT_OWNER);
        }

        comment.softDelete();

        return CommunityConverter.toCommunityCommentDeleteResponse(comment.getId());
    }

    // 게시글 좋아요 로직
    @Transactional
    public CommunityLikeResponseDTO likeCommunityPost(Long userId, Long communityId) {
        Community community = getActiveCommunity(communityId);
        User user = getUser(userId);

        if (communityLikeRepository.existsById(new CommunityLikeId(community.getId(), user.getId()))) {
            throw new CommunityException(CommunityErrorCode.ALREADY_LIKED);
        }

        communityLikeRepository.save(CommunityConverter.toCommunityLike(community, user));
        community.increaseLikeCount();

        return CommunityConverter.toCommunityLikeResponse(userId, community.getLikeCount());
    }

    // 게시글 좋아요 취소 로직
    @Transactional
    public CommunityLikeResponseDTO unlikeCommunityPost(Long userId, Long communityId) {
        Community community = getActiveCommunity(communityId);

        CommunityLike like = communityLikeRepository.findById(new CommunityLikeId(community.getId(), userId))
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.NOT_LIKED));

        communityLikeRepository.delete(like);
        community.decreaseLikeCount();

        return CommunityConverter.toCommunityLikeResponse(userId, community.getLikeCount());
    }

    // 게시글 삭제 로직(소프트 삭제)
    @Transactional
    public CommunityDeleteResponseDTO deleteCommunityPost(Long userId, Long communityId) {
        Community community = communityRepository.findByIdAndDeletedAtIsNull(communityId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.COMMUNITY_NOT_FOUND));

        if (!community.getUser().getId().equals(userId)) {
            throw new CommunityException(CommunityErrorCode.NOT_POST_OWNER);
        }

        community.softDelete();

        return CommunityConverter.toCommunityDeleteResponse(community.getId());
    }

    private void validateCreateRequest(CommunityCreateRequestDTO request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new CommunityException(CommunityErrorCode.TITLE_REQUIRED);
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new CommunityException(CommunityErrorCode.CONTENT_REQUIRED);
        }
        if (request.category() == null) {
            throw new CommunityException(CommunityErrorCode.INVALID_CATEGORY);
        }
    }

    // 게시글 이미지 S3 업로드 및 객체 키 저장 로직. 업로드한 객체 키 목록을 반환한다.
    private List<String> saveImages(Community community, List<MultipartFile> images) {
        if (images == null) {
            return List.of();
        }

        List<MultipartFile> uploadTargets = images.stream()
                .filter(image -> image != null && !image.isEmpty())
                .toList();

        List<String> imageKeys = s3Service.uploadAll(uploadTargets, IMAGE_DIRECTORY + "/" + community.getId());
        for (int order = 0; order < imageKeys.size(); order++) {
            communityImgRepository.save(CommunityConverter.toCommunityImg(community, imageKeys.get(order), order));
        }
        return imageKeys;
    }

    // 대표 이미지(display_order 최솟값)의 S3 객체 키를 조회용 Presigned URL로 변환하는 로직
    private String getRepresentativeImageUrl(Community community) {
        return communityImgRepository.findFirstByCommunityOrderByDisplayOrderAsc(community)
                .map(CommunityImg::getImageKey)
                .map(this::createImageUrl)
                .orElse(null);
    }

    // S3 객체 키를 이미지 조회용 Presigned URL로 변환하는 로직
    private String createImageUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }

        return s3Service.createPresignedUrl(imageKey);
    }

    private String getNickname(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .map(UserProfile::getNickname)
                .orElse(null);
    }

    private Community getActiveCommunity(Long communityId) {
        return communityRepository.findByIdAndDeletedAtIsNull(communityId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.COMMUNITY_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    private CommunityCategory toCategory(int code) {
        return Arrays.stream(CommunityCategory.values())
                .filter(category -> category.getCode() == code)
                .findFirst()
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.INVALID_CATEGORY));
    }
}
