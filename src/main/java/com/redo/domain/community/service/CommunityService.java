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
import java.util.Map;
import java.util.stream.Collectors;

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
    public CommunityDetailResponseDTO getCommunityPost(Long communityId) {
        Community community = communityRepository.findByIdAndDeletedAtIsNull(communityId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.COMMUNITY_NOT_FOUND));

        UserProfile profile = getProfile(community.getUser().getId());

        return CommunityConverter.toCommunityDetailResponse(
                community,
                getNickname(profile),
                getProfileImageUrl(profile),
                getCharacterCode(profile),
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

        // 등록 응답에는 만료되는 Presigned URL 대신 저장된 S3 객체 키를 그대로 담는다.
        // (등록 직후 즉시 조회 용도가 아니며, 조회 시점에 상세/목록 API가 Presigned URL을 새로 발급한다.)
        List<String> imageKeys = saveImages(community, request.images());

        return CommunityConverter.toCommunityCreateResponse(community, imageKeys, getNickname(getProfile(userId)));
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
                        .map(comment -> {
                            UserProfile profile = getProfile(comment.getUser().getId());

                            return CommunityConverter.toCommunityCommentResponse(
                                    comment,
                                    getNickname(profile),
                                    getProfileImageUrl(profile),
                                    getCharacterCode(profile)
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
