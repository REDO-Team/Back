package com.redo.domain.contribution.controller;

import com.redo.domain.contribution.dto.res.MyContributionResponseDTO;
import com.redo.domain.contribution.exception.code.ContributionSuccessCode;
import com.redo.domain.contribution.service.ContributionService;
import com.redo.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
