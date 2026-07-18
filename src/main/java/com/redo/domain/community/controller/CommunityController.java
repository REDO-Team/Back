package com.redo.domain.community.controller;

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
import com.redo.domain.community.dto.res.CommunityPageResponseDTO;
import com.redo.domain.community.exception.CommunityException;
import com.redo.domain.community.exception.code.CommunityErrorCode;
import com.redo.domain.community.exception.code.CommunitySuccessCode;
import com.redo.domain.community.service.CommunityService;
import com.redo.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class CommunityController {

    private static final int MIN_PAGE = 0;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;

    private final CommunityService communityService;

    // 커뮤니티 게시글 목록 조회 API
    @GetMapping
    public ApiResponse<CommunityPageResponseDTO> getCommunityPosts(
            @RequestParam(required = false) Integer category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePageRequest(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        return ApiResponse.onSuccess(
                CommunitySuccessCode.GET_COMMUNITY_POSTS_SUCCESS,
                CommunityConverter.toCommunityPageResponse(
                        communityService.getCommunityPosts(category, pageable)
                )
        );
    }

    // 커뮤니티 게시글 상세 조회 API
    @GetMapping("/{communityId}")
    public ApiResponse<CommunityDetailResponseDTO> getCommunityPost(
            @PathVariable Long communityId
    ) {
        return ApiResponse.onSuccess(
                CommunitySuccessCode.GET_COMMUNITY_POST_SUCCESS,
                communityService.getCommunityPost(communityId)
        );
    }

    // 커뮤니티 게시글 등록 API
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityCreateResponseDTO> createCommunityPost(
            @AuthenticationPrincipal Long userId,
            @ModelAttribute CommunityCreateRequestDTO request
    ) {
        return ApiResponse.onSuccess(
                CommunitySuccessCode.CREATE_COMMUNITY_POST_SUCCESS,
                communityService.createCommunityPost(userId, request)
        );
    }

    // 커뮤니티 게시글 댓글 목록 조회 API
    @GetMapping("/{communityId}/comments")
    public ApiResponse<CommunityCommentListResponseDTO> getCommunityComments(
            @PathVariable Long communityId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer length
    ) {
        if (length != null && length < MIN_PAGE_SIZE) {
            throw new CommunityException(CommunityErrorCode.INVALID_PAGE_REQUEST);
        }

        return ApiResponse.onSuccess(
                CommunitySuccessCode.GET_COMMUNITY_COMMENTS_SUCCESS,
                communityService.getCommunityComments(communityId, cursor, length)
        );
    }

    // 커뮤니티 게시글 댓글 등록 API
    @PostMapping("/{communityId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityCommentCreateResponseDTO> createCommunityComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long communityId,
            @RequestBody CommunityCommentCreateRequestDTO request
    ) {
        return ApiResponse.onSuccess(
                CommunitySuccessCode.CREATE_COMMUNITY_COMMENT_SUCCESS,
                communityService.createCommunityComment(userId, communityId, request)
        );
    }

    // 커뮤니티 게시글 댓글 삭제 API
    @DeleteMapping("/{communityId}/comment/{commentId}")
    public ApiResponse<CommunityCommentDeleteResponseDTO> deleteCommunityComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long communityId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.onSuccess(
                CommunitySuccessCode.DELETE_COMMUNITY_COMMENT_SUCCESS,
                communityService.deleteCommunityComment(userId, communityId, commentId)
        );
    }

    // 커뮤니티 게시글 좋아요 API
    @PostMapping("/{communityId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityLikeResponseDTO> likeCommunityPost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long communityId
    ) {
        return ApiResponse.onSuccess(
                CommunitySuccessCode.LIKE_COMMUNITY_POST_SUCCESS,
                communityService.likeCommunityPost(userId, communityId)
        );
    }

    // 커뮤니티 게시글 좋아요 취소 API
    @PostMapping("/{communityId}/unlike")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityLikeResponseDTO> unlikeCommunityPost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long communityId
    ) {
        return ApiResponse.onSuccess(
                CommunitySuccessCode.UNLIKE_COMMUNITY_POST_SUCCESS,
                communityService.unlikeCommunityPost(userId, communityId)
        );
    }

    // 커뮤니티 게시글 삭제 API
    @DeleteMapping("/{communityId}")
    public ApiResponse<CommunityDeleteResponseDTO> deleteCommunityPost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long communityId
    ) {
        return ApiResponse.onSuccess(
                CommunitySuccessCode.DELETE_COMMUNITY_POST_SUCCESS,
                communityService.deleteCommunityPost(userId, communityId)
        );
    }

    private void validatePageRequest(int page, int size) {
        if (page < MIN_PAGE || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new CommunityException(CommunityErrorCode.INVALID_PAGE_REQUEST);
        }
    }
}
