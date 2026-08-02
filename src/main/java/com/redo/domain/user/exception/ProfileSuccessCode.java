package com.redo.domain.user.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import com.redo.global.apiPayload.code.BaseSuccessCode;
import com.redo.global.apiPayload.code.SuccessReason;

@Getter
@AllArgsConstructor
public enum ProfileSuccessCode implements BaseSuccessCode {

    GET_PROFILE_SUCCESS(HttpStatus.OK, "USER_200_001", "프로필 조회에 성공했습니다."),
    UPDATE_NICKNAME_SUCCESS(HttpStatus.OK, "USER_200_002", "닉네임이 변경되었습니다."),
    CREATE_PROFILE_SUCCESS(HttpStatus.CREATED, "USER_201_001", "프로필 생성에 성공했습니다."),
    UPDATE_IMAGE_SUCCESS(HttpStatus.OK, "USER_200_003", "프로필 사진이 변경되었습니다."),
    UPDATE_CHARACTER_SUCCESS(HttpStatus.OK, "USER_200_004", "캐릭터가 변경되었습니다.");

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