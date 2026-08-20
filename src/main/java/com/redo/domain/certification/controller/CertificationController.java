package com.redo.domain.certification.controller;

import com.redo.domain.certification.dto.req.CertificationCreateRequestDTO;
import com.redo.domain.certification.dto.req.CertificationRetryRequestDTO;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
import com.redo.domain.certification.dto.res.CertificationHomeResponseDTO;
import com.redo.domain.certification.dto.res.CertificationRetryResponseDTO;
import com.redo.domain.certification.exception.code.CertificationSuccessCode;
import com.redo.domain.certification.service.CertificationCreateService;
import com.redo.domain.certification.service.CertificationHomeService;
import com.redo.domain.certification.service.CertificationRetryService;
import com.redo.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final CertificationRetryService certificationRetryService;

    @GetMapping
    @Operation(
            summary = "인증 홈 조회",
            description = """
                    오늘 성공 횟수, 신규 인증 제한 상태와 인증 방식별 리워드 정책을 조회합니다.

                    데모데이 정책에서는 일일 성공 횟수 제한과 성공 후 5분 대기를 적용하지
                    않습니다. `usedCount`는 `Asia/Seoul` 기준 오늘 `PASSED` 건수를 정보로만
                    제공합니다. 기존 프론트의 숫자 타입과 차단 분기를 그대로 호환하기 위해
                    `dailyLimit=100`, `remainingCount>=1`을 반환하지만 실제 일일 한도로
                    사용하지 않습니다. `policy.cooldownSeconds=0`이며 진행 중 인증만 신규
                    요청을 차단합니다. 응답 필드 구조는 데모데이 변경 전과 동일합니다.

                    | HTTP | isSuccess | code | restriction.type | 발생 조건 | 주요 필드 | 프론트 동작 |
                    | --- | --- | --- | --- | --- | --- | --- |
                    | 200 | true | `CERTIFICATION200_0` | `NONE` | 진행 중 인증 없음 | `canCertify=true`, 숫자형 `dailyLimit`/`remainingCount`, `cooldownSeconds=0` | 촬영 시작 |
                    | 200 | true | `CERTIFICATION200_9` | `PROCESSING_EXISTS` | 진행 중 인증 존재 | `canCertify=false`, `processingCertificationId`, `statusPath` | 기존 인증 상태 조회 |

                    `retryAvailableAt`은 항상 `null`, `remainingSeconds`는 항상 `0`입니다.
                    `DAILY_LIMIT_EXCEEDED`, `COOLDOWN`과 관련 200/429 코드는 반환하지 않습니다.

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
                    - 실측 기준 판정은 평균 30초 이내에 완료되며, 환경에 따라 더 길어질 수 있습니다. 정상 흐름에서는 polling하지 않습니다.
                    - 평균 30초는 안내 기준이며 hard timeout은 기본 210초입니다.
                    - HTTP 201 + `status=PASSED`이면 성공 결과 화면을 표시합니다.
                    - HTTP 201 + `status=FAILED`이면 `failureType`, `failedReason`, `retryGuide`로 실패 결과 화면을 표시합니다.
                    - VLM의 FAIL은 서버 오류가 아닌 완료된 비즈니스 결과이므로 HTTP 201, `isSuccess=true`입니다.

                    요청 제약:
                    - JWT 인증이 필요합니다.
                    - `GENERAL`: `image`, `certificationSource=GENERAL`을 보내고 `recycleGuideId`는 생략합니다.
                    - `AFTER_SEARCH`: `image`, `certificationSource=AFTER_SEARCH`, DB에 존재하는 `recycleGuideId`를 보냅니다.
                    - 실시간 촬영 여부는 프론트가 보장하며 서버는 빈 파일, 최대 크기와 이미지 형식을 검증합니다.
                    - 일일 PASSED 횟수와 최근 PASSED 후 경과 시간은 신규 생성을 차단하지 않습니다.
                    - 사용자의 기존 PROCESSING 인증은 계속 신규 생성을 차단합니다.
                    - 오늘 PASSED된 동일 guide는 기존과 같이 `DUPLICATE_GUIDE_TODAY`로 완료됩니다.

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
                    | 404 | `CERTIFICATION404_1` | `RECYCLE_GUIDE_NOT_FOUND` 또는 `GENERAL_CLASSIFICATION_NOT_MAPPED` | guide가 없거나 GENERAL 분류를 DB guide로 매핑하지 못함 | `type`, 선택 요청이면 `recycleGuideId` | 가이드 재선택/분류 실패 안내 |
                    | 409 | `CERTIFICATION409_0` | `PROCESSING_EXISTS` | 기존 PROCESSING 인증 존재 | `certificationId`, `statusPath` | 중복 POST 금지 및 기존 건 복구 안내 |
                    | 412 | `CERTIFICATION412_0` | `ACTIVE_TEMPLATE_NOT_FOUND` | 결정된 guide의 active template 없음 | `recycleGuideId` | 생성 중단 안내 |
                    | 413 | `S3_413_001` | 없음(code로 분기) | 공통 S3 최대 크기 초과 | 문자열 `errorDetail` | 작은 이미지로 다시 촬영 |
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

    @PostMapping(
            value = "/{certificationId}/retry",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "실패 인증 재촬영",
            description = """
                    `VLM_JUDGEMENT_FAILED`로 완료된 본인 인증을 새 이미지로 재검수합니다.

                    프론트 처리 방식:
                    - 사진 전송 후 이 POST의 Promise가 pending인 동안 재검수 대기 화면을 표시합니다.
                    - 실측 기준 판정은 평균 30초 이내에 완료되며, 환경에 따라 더 길어질 수 있습니다. 정상 흐름에서는 polling하지 않습니다.
                    - 평균 30초는 안내 기준이며 hard timeout은 기본 210초입니다.
                    - HTTP 200 + `status=PASSED`이면 성공 결과 화면을 표시합니다.
                    - HTTP 200 + `status=FAILED`이면 `failureType`, `failedReason`, `retryGuide`로 실패 결과 화면을 표시합니다.
                    - VLM의 FAIL과 오늘 동일 guide 거절은 완료된 비즈니스 결과이므로 HTTP 200, `isSuccess=true`입니다.

                    요청 및 정책 제약:
                    - JWT 인증이 필요하며 본인 소유 인증만 재촬영할 수 있습니다.
                    - multipart 필드는 실시간 재촬영한 `image` 하나입니다. source, guide와 reward point는 다시 받지 않습니다.
                    - `FAILED/VLM_JUDGEMENT_FAILED`만 재촬영할 수 있습니다.
                    - 일일 PASSED 횟수와 최근 PASSED 후 경과 시간은 재촬영을 차단하지 않습니다.
                    - 사용자의 다른 `PROCESSING` 인증 존재 여부는 계속 검사합니다.
                    - PASS 완료 직전 오늘 PASSED된 동일 guide 정책은 계속 검사합니다.
                    - GENERAL도 최초 판정에서 확정된 DB guide를 유지하며 품목 분류를 반복하지 않습니다.
                    - 기존 source와 reward point snapshot을 유지하고 접수된 재촬영마다 `attemptCount`가 증가합니다.

                    최종 응답에는 polling 필드가 포함되지 않습니다. PASSED에서만 기존
                    PointService를 호출하며 인증 상태, 새 AiJudgement, point_transactions
                    EARN과 사용자 총 포인트를 하나의 완료 트랜잭션으로 저장합니다.
                    VLM FAIL과 동일 guide 거절에는 포인트를 적립하지 않습니다.
                    응답 DTO는 null 필드를 생략하므로 PASSED의 `failureType`,
                    `failedReason`, `retryPath`와 FAILED의 비해당 필드는 JSON에 없을 수
                    있습니다. `retryGuide`는 항상 배열, `earnedPoint`는 항상 숫자입니다.

                    | HTTP | code | type/상태 | 발생 조건 | 주요 필드 | 프론트 동작 |
                    | --- | --- | --- | --- | --- | --- |
                    | 200 | `CERTIFICATION200_10` | `PASSED` | 재촬영 VLM PASS와 포인트 적립 완료 | `attemptCount`, guide/item/category, `earnedPoint`, `judgedAt` | 성공 결과 표시 |
                    | 200 | `CERTIFICATION200_11` | `FAILED / VLM_JUDGEMENT_FAILED` | 재촬영 이미지가 다시 VLM FAIL | `failedReason`, `retryGuide`, `retryAllowed=true`, `retryPath` | 실패 사유와 재촬영 안내 |
                    | 200 | `CERTIFICATION200_11` | `FAILED / DUPLICATE_GUIDE_TODAY` | 오늘 PASSED된 동일 guide | `retryAllowed=false`, `retryPath` 없음 | 다른 품목 안내 |
                    | 400 | `S3_400_001` | code로 분기 | 이미지가 비어 있음 | 문자열 `errorDetail` | 다시 촬영 |
                    | 400 | `S3_400_002` | code로 분기 | 이미지 형식이 지원되지 않음 | 문자열 `errorDetail` | 지원 형식 안내 |
                    | 400 | `POINT_400_004` | code로 분기 | 적립 후 사용자 총 포인트 정수 한도 초과 | 문자열 `errorDetail` | 적립 실패 안내/문의 |
                    | 404 | `CERTIFICATION404_0` | `CERTIFICATION_NOT_FOUND` | 인증 없음 또는 소유권 불일치 | `type` | 이전 화면으로 이동 |
                    | 409 | `CERTIFICATION409_0` | `PROCESSING_EXISTS` | 사용자의 다른 인증이 진행 중 | `certificationId`, `statusPath` | 중복 요청 금지 |
                    | 409 | `CERTIFICATION409_2` | `PASSED_NOT_RETRYABLE` | 이미 PASSED인 인증 | `type` | 성공 결과 안내 |
                    | 409 | `CERTIFICATION409_3` | `RETRY_PROCESSING` | 대상 인증이 이미 PROCESSING | `type` | 같은 retry 중복 POST 금지 |
                    | 409 | `CERTIFICATION409_4` | `RETRY_NOT_ALLOWED` | VLM 실패가 아닌 FAILED 인증 | `type` | 다른 품목 인증 안내 |
                    | 412 | `CERTIFICATION412_0` | `ACTIVE_TEMPLATE_NOT_FOUND` | 기존 guide의 active template 없음 | `recycleGuideId` | 기능 준비 상태 안내 |
                    | 413 | `S3_413_001` | code로 분기 | 공통 S3 최대 크기 초과 | 문자열 `errorDetail` | 작은 이미지로 재촬영 |
                    | 500 | `S3_500_001` | code로 분기 | S3 업로드 실패 | 문자열 `errorDetail` | 잠시 후 재시도 |
                    | 500 | `CERTIFICATION500_0` | `IMAGE_READ_FAILED` | 재촬영 이미지 조회 실패 | `type` | 시스템 오류 안내 |
                    | 500 | `GEMINI_500_001` | code로 분기 | Gemini JSON 응답 계약 위반 | 문자열 `errorDetail` | 시스템 오류 안내 |
                    | 502 | `GEMINI_502_001` | code로 분기 | Gemini provider 호출 실패 | 문자열 `errorDetail` | 잠시 후 재시도 |
                    | 503 | `GEMINI_503_001` | code로 분기 | Gemini 기능 비활성화 | 문자열 `errorDetail` | 기능 준비 중 안내 |
                    | 503 | `CERTIFICATION503_1` | `JUDGEMENT_OVERLOADED` | 판정 executor/queue 포화 | `type` | 잠시 후 재시도 |
                    | 504 | `GEMINI_504_001` | code로 분기 | 전체 판정 제한 시간(기본 210초) 초과 | 문자열 `errorDetail` | timeout 안내 후 다시 재촬영 |

                    provider/JSON/서버 오류 또는 timeout이 발생하면 기존 인증을 삭제하지
                    않고 재촬영 직전 `FAILED/VLM_JUDGEMENT_FAILED` 상태와 이전 이미지를
                    복원합니다. 실패한 요청은 attempt를 소비하지 않으며 새 이미지만
                    best-effort로 삭제합니다. 운영에서는
                    `Gemini timeout < certification 210초 < proxy/client timeout` 관계를
                    만족해야 합니다.
                    """
    )
    public CompletableFuture<ApiResponse<CertificationRetryResponseDTO>> retry(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long certificationId,
            @ModelAttribute CertificationRetryRequestDTO request
    ) {
        return certificationRetryService.retry(userId, certificationId, request)
                .thenApply(response -> ApiResponse.onSuccess(
                        CertificationSuccessCode.fromRetry(response.status()),
                        response
                ));
    }
}
