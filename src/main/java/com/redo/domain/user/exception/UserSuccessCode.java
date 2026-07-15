package com.redo.domain.user.exception;

import com.redo.global.apiPayload.code.BaseSuccessCode;
import com.redo.global.apiPayload.code.SuccessReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserSuccessCode implements BaseSuccessCode {

    CHECK_LOGIN_ID_SUCCESS(HttpStatus.OK,
            "USER_200_001",
            "아이디 중복 확인에 성공했습니다."),

    SEND_EMAIL_VERIFICATION_SUCCESS(HttpStatus.OK,
            "USER_200_002",
            "이메일 인증번호 발송에 성공했습니다."),

    VERIFY_EMAIL_SUCCESS(HttpStatus.OK,
            "USER_200_003",
            "이메일 인증에 성공했습니다."),

    SIGNUP_SUCCESS(HttpStatus.CREATED,
            "USER_201_001",
            "회원가입에 성공했습니다."),

    CREATE_PROFILE_SUCCESS(HttpStatus.CREATED,
            "USER_201_002",
            "프로필 생성에 성공했습니다."),

    GET_MY_INFO_SUCCESS(HttpStatus.OK,
            "USER_200_004",
            "프로필 조회에 성공했습니다."),

    UPDATE_PROFILE_IMAGE_SUCCESS(HttpStatus.OK,
            "USER_200_005",
            "프로필 사진 수정에 성공했습니다."),

    UPDATE_NICKNAME_SUCCESS(HttpStatus.OK,
            "USER_200_006",
            "닉네임 수정에 성공했습니다."),

    GET_WITHDRAWAL_REASONS_SUCCESS(HttpStatus.OK,
            "USER_200_007",
            "탈퇴 사유 목록 조회에 성공했습니다."),

    WITHDRAW_USER_SUCCESS(HttpStatus.OK,
            "USER_200_008",
            "회원 탈퇴에 성공했습니다.");

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
