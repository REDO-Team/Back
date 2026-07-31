package com.redo.domain.certification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.domain.certification.dto.CertificationJudgementCommand;
import com.redo.domain.certification.dto.CertificationJudgementContext;
import com.redo.domain.certification.dto.CertificationJudgementPreparation;
import com.redo.domain.certification.dto.CertificationVlmResult;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
import com.redo.domain.certification.dto.res.CertificationErrorDetail;
import com.redo.domain.certification.entity.AiJudgement;
import com.redo.domain.certification.entity.Certification;
import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.exception.CertificationException;
import com.redo.domain.certification.repository.AiJudgementRepository;
import com.redo.domain.certification.repository.CertificationRepository;
import com.redo.domain.certification.service.policy.CertificationPolicyEvaluator;
import com.redo.domain.certification.service.policy.CertificationPolicyResult;
import com.redo.domain.point.service.PointService;
import com.redo.domain.recycleGuide.dto.ActiveRecycleJudgementTemplate;
import com.redo.domain.recycleGuide.entity.RecycleGuide;
import com.redo.domain.recycleGuide.repository.RecycleGuideRepository;
import com.redo.domain.recycleGuide.service.RecycleJudgementTemplateProvider;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.exception.UserErrorCode;
import com.redo.domain.user.repository.UserRepository;
import com.redo.global.apiPayload.exception.GeneralException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.redo.domain.certification.config.CertificationTimeConfig.CERTIFICATION_CLOCK;
import static com.redo.domain.certification.config.CertificationTimeConfig.SEOUL_ZONE;
import static com.redo.domain.certification.exception.code.CertificationErrorCode.ACTIVE_TEMPLATE_NOT_FOUND;
import static com.redo.domain.certification.exception.code.CertificationErrorCode.COOLDOWN;
import static com.redo.domain.certification.exception.code.CertificationErrorCode.DAILY_LIMIT_EXCEEDED;
import static com.redo.domain.certification.exception.code.CertificationErrorCode.PROCESSING_EXISTS;
import static com.redo.domain.certification.exception.code.CertificationErrorCode.RECYCLE_GUIDE_NOT_FOUND;
import static com.redo.domain.certification.service.policy.CertificationPolicyEvaluator.DAILY_LIMIT;

@Service
public class CertificationTransactionService {

    private static final String CERTIFICATION_EARN_IDEMPOTENCY_KEY_PREFIX =
            "certification:";
    private static final String CERTIFICATION_EARN_IDEMPOTENCY_KEY_SUFFIX =
            ":earn";
    private static final String DUPLICATE_REASON = "오늘 이미 인증한 동일 가이드입니다.";
    private static final List<String> DUPLICATE_RETRY_GUIDE =
            List.of("오늘 인증하지 않은 다른 품목을 촬영해 주세요.");

    private final UserRepository userRepository;
    private final CertificationRepository certificationRepository;
    private final AiJudgementRepository aiJudgementRepository;
    private final RecycleGuideRepository recycleGuideRepository;
    private final RecycleJudgementTemplateProvider templateProvider;
    private final CertificationPolicyEvaluator policyEvaluator;
    private final PointService pointService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CertificationTransactionService(
            UserRepository userRepository,
            CertificationRepository certificationRepository,
            AiJudgementRepository aiJudgementRepository,
            RecycleGuideRepository recycleGuideRepository,
            RecycleJudgementTemplateProvider templateProvider,
            CertificationPolicyEvaluator policyEvaluator,
            PointService pointService,
            ObjectMapper objectMapper,
            @Qualifier(CERTIFICATION_CLOCK) Clock clock
    ) {
        this.userRepository = userRepository;
        this.certificationRepository = certificationRepository;
        this.aiJudgementRepository = aiJudgementRepository;
        this.recycleGuideRepository = recycleGuideRepository;
        this.templateProvider = templateProvider;
        this.policyEvaluator = policyEvaluator;
        this.pointService = pointService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public void validateAfterSearchPrerequisites(Long recycleGuideId) {
        requireGuide(recycleGuideId);
        requireActiveTemplate(recycleGuideId);
    }

    @Transactional
    public CertificationJudgementCommand createProcessing(
            Long userId,
            CertificationSource source,
            Long recycleGuideId,
            String imageKey
    ) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
        enforcePolicy(policyEvaluator.evaluate(userId));

        RecycleGuide recycleGuide = null;
        if (source == CertificationSource.AFTER_SEARCH) {
            recycleGuide = requireGuide(recycleGuideId);
            requireActiveTemplate(recycleGuideId);
        }

        Certification certification = Certification.create(
                user,
                recycleGuide,
                imageKey,
                source
        );
        certificationRepository.saveAndFlush(certification);
        return new CertificationJudgementCommand(
                certification.getId(),
                userId,
                source,
                imageKey,
                recycleGuideId
        );
    }

    @Transactional
    public CertificationJudgementPreparation prepareJudgement(
            CertificationJudgementCommand command,
            Long classifiedRecycleGuideId
    ) {
        Certification certification = requireProcessing(command);
        RecycleGuide recycleGuide;
        if (command.source() == CertificationSource.GENERAL) {
            recycleGuide = requireGuide(classifiedRecycleGuideId);
            certification.assignRecycleGuide(recycleGuide);
        } else {
            recycleGuide = certification.getRecycleGuide();
        }

        LocalDateTime now = now();
        LocalDate today = now.toLocalDate();
        boolean duplicate = certificationRepository
                .existsByUserIdAndRecycleGuideIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
                        command.userId(),
                        recycleGuide.getId(),
                        CertificationStatus.PASSED,
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay()
                );
        if (duplicate) {
            certification.rejectDuplicateGuide(now);
            return CertificationJudgementPreparation.completed(toDuplicateResponse(
                    certification,
                    recycleGuide
            ));
        }

        ActiveRecycleJudgementTemplate template =
                requireActiveTemplate(recycleGuide.getId());
        return CertificationJudgementPreparation.ready(new CertificationJudgementContext(
                certification.getId(),
                command.userId(),
                certification.getCertificationSource(),
                recycleGuide.getId(),
                recycleGuide.getName(),
                recycleGuide.getRecycleCategory() == null
                        ? null
                        : recycleGuide.getRecycleCategory().getName(),
                certification.getRewardPoint(),
                template
        ));
    }

    @Transactional
    public CertificationCreateResponseDTO completeJudgement(
            CertificationJudgementContext context,
            CertificationVlmResult result
    ) {
        Certification certification = certificationRepository
                .findByIdAndUserIdForUpdate(
                        context.certificationId(),
                        context.userId()
                )
                .filter(value -> value.getStatus() == CertificationStatus.PROCESSING)
                .orElseThrow(() -> new IllegalStateException(
                        "Certification is no longer processing"
                ));
        LocalDateTime judgedAt = now();
        String retryGuideJson = toJson(result.retryGuide());

        aiJudgementRepository.save(AiJudgement.create(
                certification,
                context.template().id(),
                result.result(),
                result.reason(),
                retryGuideJson,
                result.rawResponseJson()
        ));
        certification.complete(result.result(), judgedAt);

        boolean passed = certification.getStatus() == CertificationStatus.PASSED;
        if (passed) {
            pointService.earnPoint(
                    context.userId(),
                    certification.getId(),
                    certification.getCertificationSource(),
                    certificationEarnIdempotencyKey(certification.getId())
            );
        }
        return new CertificationCreateResponseDTO(
                certification.getId(),
                certification.getStatus(),
                certification.getFailureType(),
                context.recycleGuideId(),
                context.itemName(),
                context.categoryName(),
                passed ? context.rewardPoint() : 0,
                passed ? null : result.reason(),
                passed ? List.of() : result.retryGuide(),
                !passed,
                passed ? null : "/api/certification/" + certification.getId() + "/retry",
                judgedAt
        );
    }

    @Transactional
    public boolean deleteIfProcessing(CertificationJudgementCommand command) {
        return certificationRepository
                .findByIdAndUserIdForUpdate(
                        command.certificationId(),
                        command.userId()
                )
                .filter(value -> value.getStatus() == CertificationStatus.PROCESSING)
                .map(value -> {
                    certificationRepository.delete(value);
                    certificationRepository.flush();
                    return true;
                })
                .orElse(false);
    }

    private Certification requireProcessing(CertificationJudgementCommand command) {
        return certificationRepository
                .findByIdAndUserIdForUpdate(
                        command.certificationId(),
                        command.userId()
                )
                .filter(value -> value.getStatus() == CertificationStatus.PROCESSING)
                .orElseThrow(() -> new IllegalStateException(
                        "Certification is no longer processing"
                ));
    }

    private RecycleGuide requireGuide(Long recycleGuideId) {
        return recycleGuideRepository.findById(recycleGuideId)
                .orElseThrow(() -> new CertificationException(
                        RECYCLE_GUIDE_NOT_FOUND,
                        CertificationErrorDetail.guide(
                                "RECYCLE_GUIDE_NOT_FOUND",
                                recycleGuideId
                        )
                ));
    }

    private ActiveRecycleJudgementTemplate requireActiveTemplate(Long recycleGuideId) {
        return templateProvider.findActiveByRecycleGuideId(recycleGuideId)
                .orElseThrow(() -> new CertificationException(
                        ACTIVE_TEMPLATE_NOT_FOUND,
                        CertificationErrorDetail.guide(
                                "ACTIVE_TEMPLATE_NOT_FOUND",
                                recycleGuideId
                        )
                ));
    }

    private void enforcePolicy(CertificationPolicyResult policy) {
        if (policy.type() == CertificationRestrictionType.NONE) {
            return;
        }

        throw switch (policy.type()) {
            case DAILY_LIMIT_EXCEEDED -> new CertificationException(
                    DAILY_LIMIT_EXCEEDED,
                    CertificationErrorDetail.dailyLimit(DAILY_LIMIT, policy.usedCount())
            );
            case PROCESSING_EXISTS -> new CertificationException(
                    PROCESSING_EXISTS,
                    CertificationErrorDetail.processing(
                            policy.processingCertificationId(),
                            policy.statusPath()
                    )
            );
            case COOLDOWN -> new CertificationException(
                    COOLDOWN,
                    CertificationErrorDetail.cooldown(
                            policy.retryAvailableAt(),
                            policy.remainingSeconds()
                    )
            );
            case NONE -> throw new IllegalStateException(
                    "NONE restriction must not create an exception"
            );
        };
    }

    private CertificationCreateResponseDTO toDuplicateResponse(
            Certification certification,
            RecycleGuide recycleGuide
    ) {
        return new CertificationCreateResponseDTO(
                certification.getId(),
                certification.getStatus(),
                certification.getFailureType(),
                recycleGuide.getId(),
                recycleGuide.getName(),
                recycleGuide.getRecycleCategory() == null
                        ? null
                        : recycleGuide.getRecycleCategory().getName(),
                0,
                DUPLICATE_REASON,
                DUPLICATE_RETRY_GUIDE,
                false,
                null,
                certification.getJudgedAt()
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), SEOUL_ZONE);
    }

    private String certificationEarnIdempotencyKey(Long certificationId) {
        return CERTIFICATION_EARN_IDEMPOTENCY_KEY_PREFIX
                + certificationId
                + CERTIFICATION_EARN_IDEMPOTENCY_KEY_SUFFIX;
    }

    private String toJson(List<String> retryGuide) {
        try {
            return objectMapper.writeValueAsString(retryGuide);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize retry guide", exception);
        }
    }
}
