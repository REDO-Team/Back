package com.redo.domain.community.service;

import com.redo.domain.community.converter.CommunityConverter;
import com.redo.domain.community.dto.req.CommunityCommentCreateRequestDTO;
import com.redo.domain.community.dto.req.CommunityCommentUpdateRequestDTO;
import com.redo.domain.community.dto.req.CommunityCreateRequestDTO;
import com.redo.domain.community.dto.req.CommunityUpdateRequestDTO;
import com.redo.domain.community.dto.res.CommunityCommentCreateResponseDTO;
import com.redo.domain.community.dto.res.CommunityCommentDeleteResponseDTO;
import com.redo.domain.community.dto.res.CommunityCommentListResponseDTO;
import com.redo.domain.community.dto.res.CommunityCommentUpdateResponseDTO;
import com.redo.domain.community.dto.res.CommunityCreateResponseDTO;
import com.redo.domain.community.dto.res.CommunityDeleteResponseDTO;
import com.redo.domain.community.dto.res.CommunityDetailResponseDTO;
import com.redo.domain.community.dto.res.CommunityImageResponseDTO;
import com.redo.domain.community.dto.res.CommunityLikeResponseDTO;
import com.redo.domain.community.dto.res.CommunityResponseDTO;
import com.redo.domain.community.dto.res.CommunityUpdateResponseDTO;
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
import com.redo.global.s3.exception.S3Exception;
import com.redo.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    // 전체 보기 전용 카테고리 코드(특정 카테고리 필터 없이 전체 조회)
    private static final int CATEGORY_ALL = 0;

    // 게시글 이미지가 업로드되는 S3 디렉터리 접두어
    private static final String IMAGE_DIRECTORY = "community";

    // 첫 번째 첨부 이미지의 display_order
    private static final int FIRST_DISPLAY_ORDER = 0;

    private final CommunityRepository communityRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityImgRepository communityImgRepository;
    private final CommunityLikeRepository communityLikeRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final S3Service s3Service;

    // 게시글 목록 조회 로직(댓글 수/대표 이미지는 게시글별 단건 쿼리 대신 IN 조회로 한 번에 가져온다)
    public Page<CommunityResponseDTO> getCommunityPosts(Integer category, Pageable pageable) {
        Page<Community> communities = (category == null || category == CATEGORY_ALL)
                ? communityRepository.findByDeletedAtIsNull(pageable)
                : communityRepository.findByCategoryAndDeletedAtIsNull(toCategory(category), pageable);

        Map<Long, Long> commentCounts = getCommentCounts(communities.getContent());
        Map<Long, String> representativeImageKeys = getRepresentativeImageKeys(communities.getContent());
        Map<Long, CommunityRepository.CommunityWriter> writers = getWriters(communities.getContent());

        return communities.map(community -> {
            CommunityRepository.CommunityWriter writer = writers.get(community.getId());

            return CommunityConverter.toCommunityResponse(
                    community,
                    commentCounts.getOrDefault(community.getId(), 0L),
                    createImageUrl(representativeImageKeys.get(community.getId())),
                    writer == null ? null : writer.getNickname(),
                    writer == null ? null : createImageUrl(writer.getProfileImageKey()),
                    writer == null ? null : writer.getCharacterCode()
            );
        });
    }

    // 게시글 상세 조회 로직
    public CommunityDetailResponseDTO getCommunityPost(Long userId, Long communityId) {
        Community community = communityRepository.findByIdAndDeletedAtIsNull(communityId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.COMMUNITY_NOT_FOUND));

        UserProfile profile = getProfile(community.getUser().getId());

        return CommunityConverter.toCommunityDetailResponse(
                community,
                getNickname(profile),
                getProfileImageUrl(profile),
                getCharacterCode(profile),
                getImages(community),
                communityCommentRepository.countByCommunityAndDeletedAtIsNull(community),
                isLiked(userId, community),
                isMine(userId, community)
        );
    }

    // 조회자가 해당 게시글에 좋아요를 눌렀는지 판별하는 로직
    private boolean isLiked(Long userId, Community community) {
        return userId != null
                && communityLikeRepository.existsById(new CommunityLikeId(community.getId(), userId));
    }

    // 조회자가 해당 게시글의 작성자인지 판별하는 로직
    private boolean isMine(Long userId, Community community) {
        return userId != null && userId.equals(community.getUser().getId());
    }

    // 조회자가 해당 댓글의 작성자인지 판별하는 로직
    private boolean isMine(Long userId, CommunityComment comment) {
        return userId != null && userId.equals(comment.getUser().getId());
    }

    // 게시글 등록 로직
    @Transactional
    public CommunityCreateResponseDTO createCommunityPost(Long userId, CommunityCreateRequestDTO request) {
        validatePostRequest(request.category(), request.title(), request.content());
        CommunityCategory category = toCategory(request.category());
        User user = getUser(userId);

        Community community = communityRepository.save(
                CommunityConverter.toCommunity(user, category, request.title(), request.content())
        );

        // 등록 응답에는 만료되는 Presigned URL 대신 저장된 S3 객체 키를 그대로 담는다.
        // (등록 직후 즉시 조회 용도가 아니며, 조회 시점에 상세/목록 API가 Presigned URL을 새로 발급한다.)
        List<String> imageKeys = saveImages(community, request.images(), FIRST_DISPLAY_ORDER);

        return CommunityConverter.toCommunityCreateResponse(community, imageKeys, getNickname(getProfile(userId)));
    }

    // 게시글 수정 로직(작성자만 가능. 첨부 이미지는 삭제 대상만 지우고 새 이미지를 뒤에 추가한다)
    @Transactional
    public CommunityUpdateResponseDTO updateCommunityPost(
            Long userId,
            Long communityId,
            CommunityUpdateRequestDTO request
    ) {
        validatePostRequest(request.category(), request.title(), request.content());

        Community community = getActiveCommunity(communityId);
        if (!community.getUser().getId().equals(userId)) {
            throw new CommunityException(CommunityErrorCode.NOT_POST_OWNER);
        }

        community.update(toCategory(request.category()), request.title(), request.content());

        // 제목/본문 변경 없이 첨부 이미지만 바뀌면 게시글 컬럼이 그대로라 UPDATE 가 나가지 않으므로 수정 시각을 직접 갱신한다.
        if (editImages(community, request.deleteImageIds(), request.images())) {
            community.touch();
        }

        // 응답에 갱신된 updatedAt(@PreUpdate 로 채워진다)을 담기 위해 변경 내용을 먼저 반영한다.
        communityRepository.flush();

        return CommunityConverter.toCommunityUpdateResponse(community, getImages(community));
    }

    // 댓글 목록 조회 로직(comment ID 기준 커서 페이징, cursor/length 없으면 전체 반환)
    public CommunityCommentListResponseDTO getCommunityComments(
            Long userId,
            Long communityId,
            Long cursor,
            Integer length
    ) {
        Community community = getActiveCommunity(communityId);

        List<CommunityComment> comments = communityCommentRepository
                .findByCommunityAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
                        community,
                        cursor == null ? 0L : cursor,
                        length == null ? Pageable.unpaged() : PageRequest.of(0, length)
                );

        return CommunityConverter.toCommunityCommentListResponse(
                comments.stream()
                        .map(comment -> {
                            UserProfile profile = getProfile(comment.getUser().getId());

                            return CommunityConverter.toCommunityCommentResponse(
                                    comment,
                                    getNickname(profile),
                                    getProfileImageUrl(profile),
                                    getCharacterCode(profile),
                                    isMine(userId, comment)
                            );
                        })
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

    // 댓글 수정 로직(작성자만 가능. 내용만 갱신한다)
    @Transactional
    public CommunityCommentUpdateResponseDTO updateCommunityComment(
            Long userId,
            Long communityId,
            Long commentId,
            CommunityCommentUpdateRequestDTO request
    ) {
        if (request.comment() == null || request.comment().isBlank()) {
            throw new CommunityException(CommunityErrorCode.COMMENT_REQUIRED);
        }

        CommunityComment comment = getOwnedComment(userId, communityId, commentId);

        comment.update(request.comment());

        // 응답에 갱신된 updatedAt(@PreUpdate 로 채워진다)을 담기 위해 변경 내용을 먼저 반영한다.
        communityCommentRepository.flush();

        return CommunityConverter.toCommunityCommentUpdateResponse(comment);
    }

    // 댓글 삭제 로직(소프트 삭제)
    @Transactional
    public CommunityCommentDeleteResponseDTO deleteCommunityComment(Long userId, Long communityId, Long commentId) {
        CommunityComment comment = getOwnedComment(userId, communityId, commentId);

        comment.softDelete();

        return CommunityConverter.toCommunityCommentDeleteResponse(comment.getId());
    }

    // 수정/삭제 공통: 해당 게시글에 달린 삭제되지 않은 본인 댓글을 조회하는 로직
    private CommunityComment getOwnedComment(Long userId, Long communityId, Long commentId) {
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

        return comment;
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
        // 동시 요청에서 갱신 유실이 없도록 엔티티 증감 대신 DB 원자적 UPDATE 를 사용한다.
        communityRepository.increaseLikeCount(community.getId());

        return CommunityConverter.toCommunityLikeResponse(
                userId,
                communityRepository.findLikeCountById(community.getId())
        );
    }

    // 게시글 좋아요 취소 로직
    @Transactional
    public CommunityLikeResponseDTO unlikeCommunityPost(Long userId, Long communityId) {
        Community community = getActiveCommunity(communityId);

        CommunityLike like = communityLikeRepository.findById(new CommunityLikeId(community.getId(), userId))
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.NOT_LIKED));

        communityLikeRepository.delete(like);
        // 동시 요청에서 갱신 유실이 없도록 엔티티 증감 대신 DB 원자적 UPDATE 를 사용한다.
        communityRepository.decreaseLikeCount(community.getId());

        return CommunityConverter.toCommunityLikeResponse(
                userId,
                communityRepository.findLikeCountById(community.getId())
        );
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

    // 게시글 등록/수정 공통 필수값 검증 로직
    private void validatePostRequest(Integer category, String title, String content) {
        if (title == null || title.isBlank()) {
            throw new CommunityException(CommunityErrorCode.TITLE_REQUIRED);
        }
        if (content == null || content.isBlank()) {
            throw new CommunityException(CommunityErrorCode.CONTENT_REQUIRED);
        }
        if (category == null) {
            throw new CommunityException(CommunityErrorCode.INVALID_CATEGORY);
        }
    }

    // 게시글 이미지 S3 업로드 및 객체 키 저장 로직. 업로드한 객체 키 목록을 반환한다.
    // startOrder 부터 display_order 를 순서대로 매겨 저장한다(등록은 0, 수정은 기존 이미지 다음 순서).
    private List<String> saveImages(Community community, List<MultipartFile> images, int startOrder) {
        if (images == null) {
            return List.of();
        }

        List<MultipartFile> uploadTargets = images.stream()
                .filter(image -> image != null && !image.isEmpty())
                .toList();

        List<String> imageKeys = s3Service.uploadAll(uploadTargets, IMAGE_DIRECTORY + "/" + community.getId());
        for (int index = 0; index < imageKeys.size(); index++) {
            communityImgRepository.save(
                    CommunityConverter.toCommunityImg(community, imageKeys.get(index), startOrder + index)
            );
        }
        return imageKeys;
    }

    // 첨부 이미지 부분 수정 로직(deleteImageIds 로 지정한 이미지만 삭제하고, 새 이미지는 기존 이미지 뒤에 추가한다)
    // 실제로 삭제되거나 추가된 이미지가 있으면 true 를 반환한다.
    private boolean editImages(Community community, List<Long> deleteImageIds, List<MultipartFile> images) {
        List<CommunityImg> currentImages = communityImgRepository.findByCommunityOrderByDisplayOrderAsc(community);

        List<String> deletedImageKeys = deleteImages(currentImages, deleteImageIds);
        // 남은 이미지와 순서가 겹치지 않도록 기존 display_order 최댓값 다음부터 이어서 저장한다.
        List<String> addedImageKeys = saveImages(community, images, nextDisplayOrder(currentImages));

        // S3 객체 정리는 DB 반영 이후에 수행하고, 실패하더라도 수정 자체는 성공으로 처리한다(고아 객체는 로그로 남긴다).
        try {
            s3Service.deleteAll(deletedImageKeys);
        } catch (S3Exception exception) {
            log.error("Failed to delete removed community images from S3. communityId={}, keys={}",
                    community.getId(), deletedImageKeys, exception);
        }

        return !deletedImageKeys.isEmpty() || !addedImageKeys.isEmpty();
    }

    // 삭제 대상 이미지 행을 지우고, 정리해야 할 S3 객체 키를 반환하는 로직
    private List<String> deleteImages(List<CommunityImg> currentImages, List<Long> deleteImageIds) {
        if (deleteImageIds == null || deleteImageIds.isEmpty()) {
            return List.of();
        }

        Set<Long> targetIds = deleteImageIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<CommunityImg> targets = currentImages.stream()
                .filter(image -> targetIds.contains(image.getId()))
                .toList();

        // 다른 게시글의 이미지 id 나 이미 삭제된 id 로 접근하는 경우는 존재하지 않는 것으로 처리한다.
        if (targets.size() != targetIds.size()) {
            throw new CommunityException(CommunityErrorCode.COMMUNITY_IMAGE_NOT_FOUND);
        }

        // Hibernate 는 같은 flush 안에서 insert 를 delete 보다 먼저 수행하므로, 삭제를 먼저 반영시킨다.
        communityImgRepository.deleteAll(targets);
        communityImgRepository.flush();

        return targets.stream()
                .map(CommunityImg::getImageKey)
                .toList();
    }

    // 새로 추가할 이미지가 사용할 시작 display_order 를 계산하는 로직
    private int nextDisplayOrder(List<CommunityImg> currentImages) {
        return currentImages.stream()
                .mapToInt(CommunityImg::getDisplayOrder)
                .max()
                .orElse(-1) + 1;
    }

    // 목록의 게시글별 삭제되지 않은 댓글 수를 한 번의 집계 쿼리로 조회하는 로직
    private Map<Long, Long> getCommentCounts(List<Community> communities) {
        if (communities.isEmpty()) {
            return Map.of();
        }

        return communityCommentRepository.findCommentCountsByCommunities(communities).stream()
                .collect(Collectors.toMap(
                        CommunityCommentRepository.CommunityCommentCount::getCommunityId,
                        CommunityCommentRepository.CommunityCommentCount::getCommentCount
                ));
    }

    // 목록의 게시글별 작성자 프로필을 한 번의 쿼리로 조회하는 로직
    // 프로필이 없는 작성자도 projection 자체는 반환되므로(각 필드가 null) Map 수집에서 제외하지 않는다.
    private Map<Long, CommunityRepository.CommunityWriter> getWriters(List<Community> communities) {
        if (communities.isEmpty()) {
            return Map.of();
        }

        return communityRepository.findWritersByCommunities(communities).stream()
                .collect(Collectors.toMap(
                        CommunityRepository.CommunityWriter::getCommunityId,
                        writer -> writer
                ));
    }

    // 목록의 게시글별 대표 이미지(display_order 최솟값) 객체 키를 한 번의 쿼리로 조회하는 로직
    private Map<Long, String> getRepresentativeImageKeys(List<Community> communities) {
        if (communities.isEmpty()) {
            return Map.of();
        }

        return communityImgRepository.findByCommunityInOrderByDisplayOrderAsc(communities).stream()
                .collect(Collectors.toMap(
                        image -> image.getCommunity().getId(),// 키 추출 람다
                        CommunityImg::getImageKey,                          // 값 추출 람다
                        (first, duplicate) -> first            // 대표 이미지를 제외하고 모두 무시
                ));
    }

    // 상세/수정 응답용: 첨부 이미지 전체를 등록 순서(display_order)대로 id + Presigned URL 로 변환하는 로직
    private List<CommunityImageResponseDTO> getImages(Community community) {
        return communityImgRepository.findByCommunityOrderByDisplayOrderAsc(community).stream()
                .map(image -> CommunityConverter.toCommunityImageResponse(image, createImageUrl(image.getImageKey())))
                .filter(image -> Objects.nonNull(image.imageUrl()))
                .toList();
    }

    // S3 객체 키를 이미지 조회용 Presigned URL로 변환하는 로직
    private String createImageUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }

        return s3Service.createPresignedUrl(imageKey);
    }

    // 작성자 프로필을 조회하는 로직(프로필을 아직 만들지 않은 사용자는 null 을 반환한다)
    private UserProfile getProfile(Long userId) {
        return userProfileRepository.findByUserId(userId).orElse(null);
    }

    private String getNickname(UserProfile profile) {
        return profile == null ? null : profile.getNickname();
    }

    // 작성자 프로필 이미지의 S3 객체 키를 조회용 Presigned URL로 변환하는 로직
    // 프로필 이미지를 등록하지 않은 사용자는 null 이 되며, 이 경우 characterCode 로 대체 표시한다.
    private String getProfileImageUrl(UserProfile profile) {
        return profile == null ? null : createImageUrl(profile.getProfileImageKey());
    }

    private String getCharacterCode(UserProfile profile) {
        return profile == null ? null : profile.getCharacterCode();
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
