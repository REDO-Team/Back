package com.redo.domain.community.exception.code;

import com.redo.global.apiPayload.code.BaseSuccessCode;
import com.redo.global.apiPayload.code.SuccessReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommunitySuccessCode implements BaseSuccessCode {

    GET_COMMUNITY_POSTS_SUCCESS(HttpStatus.OK,
            "COMMUNITY_200_001",
            "커뮤니티 게시글 목록 조회에 성공했습니다."),

    GET_COMMUNITY_POST_SUCCESS(HttpStatus.OK,
            "COMMUNITY_200_002",
            "커뮤니티 게시글 상세 조회에 성공했습니다."),

    DELETE_COMMUNITY_POST_SUCCESS(HttpStatus.OK,
            "COMMUNITY_200_003",
            "게시글이 삭제되었습니다."),

    GET_COMMUNITY_COMMENTS_SUCCESS(HttpStatus.OK,
            "COMMUNITY_200_004",
            "댓글 목록 조회에 성공했습니다."),

    DELETE_COMMUNITY_COMMENT_SUCCESS(HttpStatus.OK,
            "COMMUNITY_200_005",
            "댓글이 삭제되었습니다."),

    UNLIKE_COMMUNITY_POST_SUCCESS(HttpStatus.OK,
            "COMMUNITY_200_006",
            "좋아요를 취소했습니다."),

    CREATE_COMMUNITY_POST_SUCCESS(HttpStatus.CREATED,
            "COMMUNITY_201_001",
            "커뮤니티 게시글 등록에 성공했습니다."),

    LIKE_COMMUNITY_POST_SUCCESS(HttpStatus.CREATED,
            "COMMUNITY_201_002",
            "좋아요를 추가했습니다."),

    CREATE_COMMUNITY_COMMENT_SUCCESS(HttpStatus.CREATED,
            "COMMUNITY_201_003",
            "댓글 등록에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public SuccessReason getReason() {
        return SuccessReason.builder()
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}
