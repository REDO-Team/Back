package com.redo.domain.contribution.controller;

import com.redo.domain.contribution.dto.res.MyContributionResponseDTO;
import com.redo.domain.contribution.dto.res.OverallContributionResponseDTO;
import com.redo.domain.contribution.exception.ContributionException;
import com.redo.domain.contribution.exception.code.ContributionErrorCode;
import com.redo.domain.contribution.exception.code.ContributionSuccessCode;
import com.redo.domain.contribution.service.ContributionService;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.util.CursorRequestValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "기여도", description = "나의 기여도 및 전체 기여도 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contributions")
public class ContributionController {

    private final ContributionService contributionService;

    @GetMapping("/me")
    @Operation(
            summary = "나의 기여도 조회",
            description = "로그인한 사용자의 누적 성공 인증 횟수와 재활용 물품별 달성 상태를 조회합니다."
    )
    public ApiResponse<MyContributionResponseDTO> getMyContribution(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(
                ContributionSuccessCode.GET_MY_CONTRIBUTION_SUCCESS,
                contributionService.getMyContribution(userId)
        );
    }

    @GetMapping
    @Operation(
            summary = "전체 기여도 조회",
            description = "활성 사용자 수와 전체 기여도 활동 피드를 커서 기반으로 최신순 조회합니다."
    )
    public ApiResponse<OverallContributionResponseDTO> getOverallContribution(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        CursorRequestValidator.validate(
                cursor,
                size,
                () -> new ContributionException(
                        ContributionErrorCode.INVALID_CURSOR_REQUEST
                )
        );

        return ApiResponse.onSuccess(
                ContributionSuccessCode.GET_OVERALL_CONTRIBUTION_SUCCESS,
                contributionService.getOverallContribution(cursor, size)
        );
    }
}
