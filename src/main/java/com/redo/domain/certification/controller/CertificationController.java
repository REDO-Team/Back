package com.redo.domain.certification.controller;

import com.redo.domain.certification.dto.res.CertificationHomeResponseDTO;
import com.redo.domain.certification.exception.code.CertificationSuccessCode;
import com.redo.domain.certification.service.CertificationHomeService;
import com.redo.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "배출 인증", description = "배출 인증 및 인증 정책 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/certification")
public class CertificationController {

    private final CertificationHomeService certificationHomeService;

    @GetMapping
    @Operation(
            summary = "인증 홈 조회",
            description = """
                    오늘 성공 횟수, 신규 인증 제한 상태와 인증 방식별 리워드 정책을 조회합니다.

                    일일 인증 횟수는 `Asia/Seoul` 기준 오늘 `PASSED`된 인증만 집계하며,
                    최대 3회까지 포인트 적립이 가능합니다. 제한 상태도 홈 화면을 정상 조회한
                    결과이므로 모두 HTTP 200, `isSuccess=true`로 반환됩니다.

                    | code | restriction.type | 설명 |
                    | --- | --- | --- |
                    | `CERTIFICATION200_0` | `NONE` | 현재 새로운 인증을 시작할 수 있습니다. |
                    | `CERTIFICATION200_1` | `DAILY_LIMIT_EXCEEDED` | 오늘 `PASSED` 인증이 3회 이상이어서 새로운 인증을 시작할 수 없습니다. |
                    | `CERTIFICATION200_8` | `COOLDOWN` | 직전 완료 인증(`PASSED` 또는 `FAILED`) 후 5분이 지나지 않았습니다. `retryAvailableAt`, `remainingSeconds`로 재시도 가능 시점을 확인합니다. |
                    | `CERTIFICATION200_9` | `PROCESSING_EXISTS` | 진행 중인 인증이 있어 새로운 인증을 시작할 수 없습니다. `processingCertificationId`, `statusPath`로 기존 인증 상태를 조회합니다. |

                    제한 상태의 적용 우선순위는 `DAILY_LIMIT_EXCEEDED` → `PROCESSING_EXISTS`
                    → `COOLDOWN` → `NONE`입니다. 실패 인증 재촬영은 5분 제한 대상이 아니며,
                    이 API의 `COOLDOWN`은 새로운 인증 생성에 대한 안내입니다.

                    리워드 지급 후보 포인트는 쓰레기 종류와 무관하게 일반 인증(`GENERAL`) 50P,
                    검색 후 인증(`AFTER_SEARCH`) 100P입니다.
                    """
    )
    public ApiResponse<CertificationHomeResponseDTO> getHome(
            @AuthenticationPrincipal Long userId
    ) {
        CertificationHomeResponseDTO response = certificationHomeService.getHome(userId);
        CertificationSuccessCode successCode =
                CertificationSuccessCode.from(response.restriction().type());

        return ApiResponse.onSuccess(successCode, response);
    }
}
