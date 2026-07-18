package com.redo.domain.community.exception.code;

import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommunityErrorCode implements BaseErrorCode {

    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST,
            "COMMUNITY_400_001",
            "페이지 요청 값이 올바르지 않습니다."),

    INVALID_CATEGORY(HttpStatus.BAD_REQUEST,
            "COMMUNITY_400_002",
            "유효하지 않은 카테고리입니다."),

    TITLE_REQUIRED(HttpStatus.BAD_REQUEST,
            "COMMUNITY_400_003",
            "제목이 비어 있습니다."),

    CONTENT_REQUIRED(HttpStatus.BAD_REQUEST,
            "COMMUNITY_400_004",
            "게시글 내용이 비어 있습니다."),

    ALREADY_LIKED(HttpStatus.BAD_REQUEST,
            "COMMUNITY_400_005",
            "이미 좋아요를 추가했습니다."),

    NOT_LIKED(HttpStatus.BAD_REQUEST,
            "COMMUNITY_400_006",
            "좋아요가 되어있지 않습니다."),

    COMMENT_REQUIRED(HttpStatus.BAD_REQUEST,
            "COMMUNITY_400_007",
            "댓글이 비어 있습니다."),

    NOT_POST_OWNER(HttpStatus.FORBIDDEN,
            "COMMUNITY_403_001",
            "게시글 작성자가 아닙니다."),

    NOT_COMMENT_OWNER(HttpStatus.FORBIDDEN,
            "COMMUNITY_403_002",
            "댓글 작성자가 아닙니다."),

    COMMUNITY_NOT_FOUND(HttpStatus.NOT_FOUND,
            "COMMUNITY_404_001",
            "게시글이 존재하지 않습니다."),

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "COMMUNITY_404_002",
            "댓글이 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReason getReason() {
        return ErrorReason.builder()
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}
