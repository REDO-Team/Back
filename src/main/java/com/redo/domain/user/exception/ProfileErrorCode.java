package com.redo.domain.user.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;

@Getter
@AllArgsConstructor
public enum ProfileErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER_409_001", "이미 사용 중인 닉네임입니다."),
    PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409_002", "이미 프로필이 등록된 사용자입니다."),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "USER_400_001", "닉네임 형식에 맞게 입력해주세요."),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "USER_400_002", "지원하지 않는 이미지 형식입니다. (jpg, png만 가능)"),
    IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "USER_400_003", "이미지 크기는 5MB 이하만 가능합니다.");

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