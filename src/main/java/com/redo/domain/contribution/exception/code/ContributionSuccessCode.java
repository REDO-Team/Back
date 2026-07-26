package com.redo.domain.contribution.exception.code;

import com.redo.global.apiPayload.code.BaseSuccessCode;
import com.redo.global.apiPayload.code.SuccessReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ContributionSuccessCode implements BaseSuccessCode {

    GET_MY_CONTRIBUTION_SUCCESS(
            HttpStatus.OK,
            "CONTRIBUTION_200_001",
            "나의 기여도 조회에 성공했습니다."
    ),

    GET_OVERALL_CONTRIBUTION_SUCCESS(
            HttpStatus.OK,
            "CONTRIBUTION_200_002",
            "전체 기여도 조회에 성공했습니다."
    );

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
