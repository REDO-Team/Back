package com.redo.domain.certification.controller;

import com.redo.domain.certification.dto.req.CertificationCreateRequestDTO;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
import com.redo.domain.certification.dto.res.CertificationHomeResponseDTO;
import com.redo.domain.certification.exception.code.CertificationSuccessCode;
import com.redo.domain.certification.service.CertificationCreateService;
import com.redo.domain.certification.service.CertificationHomeService;
import com.redo.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@Tag(name = "배출 인증", description = "배출 인증 및 인증 정책 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/certification")
public class CertificationController {

    private final CertificationHomeService certificationHomeService;
    private final CertificationCreateService certificationCreateService;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "신규 인증 생성",
            description = """
                    실시간 촬영한 이미지로 새로운 인증 검수를 요청합니다.

                    프론트 처리 방식:
                    - 사진 전송 후 이 POST의 Promise가 pending인 동안 인증 검수 대기 화면을 표시합니다.
                    - 판정에는 통상 2~3분이 걸릴 수 있으며 정상 흐름에서는 polling하지 않습니다.
                    - HTTP 201 + `status=PASSED`이면 성공 결과 화면을 표시합니다.
                    - HTTP 201 + `status=FAILED`이면 `failureType`, `failedReason`, `retryGuide`로 실패 결과 화면을 표시합니다.
                    - VLM의 FAIL은 서버 오류가 아닌 완료된 비즈니스 결과이므로 HTTP 201, `isSuccess=true`입니다.

                    요청 제약:
                    - JWT 인증이 필요합니다.
                    - `GENERAL`: `image`, `certificationSource=GENERAL`을 보내고 `recycleGuideId`는 생략합니다.
                    - `AFTER_SEARCH`: `image`, `certificationSource=AFTER_SEARCH`, DB에 존재하는 `recycleGuideId`를 보냅니다.
                    - 실시간 촬영 여부는 프론트가 보장하며 서버는 빈 파일, 최대 크기와 이미지 형식을 검증합니다.
                    - 신규 생성 제한 우선순위는 일일 PASSED 3회 → 기존 PROCESSING → 직전 완료 후 5분입니다.
                    - 제한 시간과 일일 경계는 `Asia/Seoul` 기준입니다.
                    - 실패 인증 재촬영은 이 API가 아니며 5분 제한에서 제외됩니다.

                    최종 성공 응답에는 `pollingIntervalSeconds`, `statusPath`, `resultPath`가
                    포함되지 않습니다. PASSED에서만 `earnedPoint`가 50P(GENERAL) 또는
                    100P(AFTER_SEARCH)이며 FAILED는 0P입니다. PASSED 전환, AiJudgement 저장,
                    기존 PointService를 통한 `point_transactions` EARN 저장과 사용자 총 포인트
                    증가는 하나의 완료 트랜잭션으로 처리됩니다. 인증 ID 기반 멱등키를 사용하며,
                    포인트 적립이 실패하면 PASSED 전환도 함께 롤백됩니다.

                    | HTTP | code | type/상태 | 발생 조건 | 주요 필드 | 프론트 동작 |
                    | --- | --- | --- | --- | --- | --- |
                    | 201 | `CERTIFICATION201_0` | `PASSED` | VLM 판정 및 포인트 적립 트랜잭션 완료 | `certificationId`, guide/item/category, `earnedPoint`, `judgedAt` | 성공 결과 표시 |
                    | 201 | `CERTIFICATION201_1` | `FAILED / VLM_JUDGEMENT_FAILED` | VLM 판정 실패 | `failedReason`, `retryGuide`, `retryAllowed=true`, `retryPath` | 실패 사유와 재촬영 안내 |
                    | 201 | `CERTIFICATION201_1` | `FAILED / DUPLICATE_GUIDE_TODAY` | 오늘 PASSED된 동일 guide | `retryAllowed=false` | 다른 품목 안내 |
                    | 400 | `S3_400_001` | 없음(code로 분기) | 이미지가 비어 있음 | 문자열 `errorDetail` | 다시 촬영 |
                    | 400 | `S3_400_002` | 없음(code로 분기) | 확장자/Content-Type이 지원되지 않음 | 문자열 `errorDetail` | 지원 형식으로 다시 촬영 |
                    | 400 | `CERTIFICATION400_1` | `INVALID_SOURCE_GUIDE_CONTRACT` | source가 잘못되었거나 source별 guide 조건 위반 | `type` | 요청 필드 수정 |
                    | 400 | `POINT_400_004` | code로 분기 | 적립 후 사용자 총 포인트가 정수 한도를 초과함 | 문자열 `errorDetail` | 포인트 적립 실패 안내 및 문의 유도 |
                    | 400 | `POINT_400_007` | code로 분기 | 인증/포인트 일일 집계 불일치로 Point 도메인의 3회 제한에 걸림 | 문자열 `errorDetail` | 당일 추가 인증 차단 |
                    | 404 | `CERTIFICATION404_1` | `RECYCLE_GUIDE_NOT_FOUND` 또는 `GENERAL_CLASSIFICATION_NOT_MAPPED` | guide가 없거나 GENERAL 분류를 DB guide로 매핑하지 못함 | `type`, 선택 요청이면 `recycleGuideId` | 가이드 재선택/분류 실패 안내 |
                    | 409 | `CERTIFICATION409_0` | `PROCESSING_EXISTS` | 기존 PROCESSING 인증 존재 | `certificationId`, `statusPath` | 중복 POST 금지 및 기존 건 복구 안내 |
                    | 412 | `CERTIFICATION412_0` | `ACTIVE_TEMPLATE_NOT_FOUND` | 결정된 guide의 active template 없음 | `recycleGuideId` | 생성 중단 안내 |
                    | 413 | `S3_413_001` | 없음(code로 분기) | 공통 S3 최대 크기 초과 | 문자열 `errorDetail` | 작은 이미지로 다시 촬영 |
                    | 429 | `CERTIFICATION429_0` | `DAILY_LIMIT_EXCEEDED` | 오늘 PASSED 3회 이상 | `dailyLimit`, `usedCount` | 당일 신규 인증 차단 |
                    | 429 | `CERTIFICATION429_1` | `COOLDOWN` | 직전 PASSED/FAILED 완료 후 5분 미경과 | `retryAvailableAt`, `remainingSeconds` | 카운트다운 후 재시도 |
                    | 500 | `S3_500_001` | 없음(code로 분기) | S3 업로드 실패 | 문자열 `errorDetail` | 잠시 후 재시도 |
                    | 500 | `CERTIFICATION500_0` | `IMAGE_READ_FAILED` | 판정용 S3 이미지 조회 실패 | `type` | 시스템 오류 안내 |
                    | 500 | `GEMINI_500_001` | code로 분기 | Gemini 응답 JSON이 비어 있거나 계약 위반 | 문자열 `errorDetail` | 시스템 오류 안내 |
                    | 502 | `GEMINI_502_001` | code로 분기 | Gemini provider 호출 실패 | 문자열 `errorDetail` | 잠시 후 재시도 |
                    | 503 | `GEMINI_503_001` | code로 분기 | Gemini 기능 비활성화 | 문자열 `errorDetail` | 기능 준비 중 안내 |
                    | 503 | `CERTIFICATION503_1` | `JUDGEMENT_OVERLOADED` | 전용 판정 executor/queue 포화 | `type` | 잠시 후 재시도 |
                    | 504 | `GEMINI_504_001` | code로 분기 | 전체 판정 제한 시간(기본 210초) 초과 | 문자열 `errorDetail` | 시간 초과 안내 후 재시도 |

                    시스템 오류나 전체 timeout이면 아직 PROCESSING인 인증과 S3 이미지를
                    조건부 정리하며, 사용자 사진의 VLM 실패로 저장하지 않습니다.
                    운영에서는 `Gemini timeout < 인증 210초 < proxy/client timeout` 관계를
                    만족하도록 proxy와 프론트 HTTP client timeout을 별도로 설정해야 합니다.
                    """
    )
    public CompletableFuture<ApiResponse<CertificationCreateResponseDTO>> create(
            @AuthenticationPrincipal Long userId,
            @ModelAttribute CertificationCreateRequestDTO request
    ) {
        return certificationCreateService.create(userId, request)
                .thenApply(response -> ApiResponse.onSuccess(
                        CertificationSuccessCode.from(response.status()),
                        response
                ));
    }
}
