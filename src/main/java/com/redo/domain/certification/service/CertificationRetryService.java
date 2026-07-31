package com.redo.domain.certification.service;

import com.redo.domain.certification.config.CertificationJudgementProperties;
import com.redo.domain.certification.dto.CertificationRetryIntake;
import com.redo.domain.certification.dto.req.CertificationRetryRequestDTO;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
import com.redo.domain.certification.dto.res.CertificationErrorDetail;
import com.redo.domain.certification.dto.res.CertificationRetryResponseDTO;
import com.redo.domain.certification.exception.CertificationException;
import com.redo.domain.certification.service.image.CertificationImageStorage;
import com.redo.domain.certification.service.judgement.CertificationJudgementProcessor;
import com.redo.global.ai.gemini.exception.GeminiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

import static com.redo.domain.certification.config.CertificationJudgementConfig.JUDGEMENT_EXECUTOR;
import static com.redo.domain.certification.config.CertificationJudgementConfig.TIMEOUT_SCHEDULER;
import static com.redo.domain.certification.exception.code.CertificationErrorCode.JUDGEMENT_OVERLOADED;
import static com.redo.global.ai.gemini.exception.code.GeminiErrorCode.TIMEOUT;

@Slf4j
@Service
public class CertificationRetryService {

    private final CertificationTransactionService transactionService;
    private final CertificationImageStorage imageStorage;
    private final CertificationJudgementProcessor judgementProcessor;
    private final ThreadPoolTaskExecutor judgementExecutor;
    private final TaskScheduler timeoutScheduler;
    private final CertificationJudgementProperties properties;

    public CertificationRetryService(
            CertificationTransactionService transactionService,
            CertificationImageStorage imageStorage,
            CertificationJudgementProcessor judgementProcessor,
            @Qualifier(JUDGEMENT_EXECUTOR) ThreadPoolTaskExecutor judgementExecutor,
            @Qualifier(TIMEOUT_SCHEDULER) TaskScheduler timeoutScheduler,
            CertificationJudgementProperties properties
    ) {
        this.transactionService = transactionService;
        this.imageStorage = imageStorage;
        this.judgementProcessor = judgementProcessor;
        this.judgementExecutor = judgementExecutor;
        this.timeoutScheduler = timeoutScheduler;
        this.properties = properties;
    }

    public CompletableFuture<CertificationRetryResponseDTO> retry(
            Long userId,
            Long certificationId,
            CertificationRetryRequestDTO request
    ) {
        transactionService.validateRetryPrerequisites(userId, certificationId);

        String imageKey = imageStorage.upload(
                userId,
                request == null ? null : request.image()
        );
        CertificationRetryIntake intake;
        try {
            intake = transactionService.startRetry(
                    userId,
                    certificationId,
                    imageKey
            );
        } catch (RuntimeException exception) {
            compensateImage(imageKey);
            throw exception;
        }

        CompletableFuture<CertificationRetryResponseDTO> response =
                new CompletableFuture<>();
        Object completionMonitor = new Object();
        ScheduledFuture<?> timeoutTask;
        try {
            timeoutTask = timeoutScheduler.schedule(
                    () -> timeout(intake, response, completionMonitor),
                    Instant.now().plus(properties.timeout())
            );
        } catch (TaskRejectedException exception) {
            recoverAndCompensate(intake);
            throw overloaded();
        }
        response.whenComplete((ignored, throwable) -> timeoutTask.cancel(false));

        try {
            judgementExecutor.execute(() -> process(
                    intake,
                    response,
                    completionMonitor
            ));
        } catch (TaskRejectedException exception) {
            timeoutTask.cancel(false);
            recoverAndCompensate(intake);
            throw overloaded();
        }
        return response;
    }

    private void process(
            CertificationRetryIntake intake,
            CompletableFuture<CertificationRetryResponseDTO> response,
            Object completionMonitor
    ) {
        CertificationRetryResponseDTO result;
        try {
            CertificationCreateResponseDTO judgementResult =
                    judgementProcessor.process(intake.command());
            result = CertificationRetryResponseDTO.from(
                    judgementResult,
                    intake.attemptCount()
            );
        } catch (Exception exception) {
            fail(intake, response, completionMonitor, exception);
            return;
        } catch (Error error) {
            log.error(
                    "Fatal error during certification retry judgement. certificationId={}",
                    intake.command().certificationId(),
                    error
            );
            fail(intake, response, completionMonitor, error);
            throw error;
        }

        boolean responseCompleted;
        synchronized (completionMonitor) {
            responseCompleted = response.complete(result);
        }
        if (responseCompleted) {
            deletePreviousImage(intake);
        }
    }

    private void timeout(
            CertificationRetryIntake intake,
            CompletableFuture<CertificationRetryResponseDTO> response,
            Object completionMonitor
    ) {
        RecoveryResult recoveryResult;
        synchronized (completionMonitor) {
            if (response.isDone()) {
                return;
            }

            recoveryResult = restoreRetry(intake);
            if (recoveryResult == RecoveryResult.NOT_PROCESSING) {
                return;
            }
            response.completeExceptionally(new GeminiException(TIMEOUT));
        }
        compensateNewImage(intake, recoveryResult);
    }

    private void fail(
            CertificationRetryIntake intake,
            CompletableFuture<CertificationRetryResponseDTO> response,
            Object completionMonitor,
            Throwable exception
    ) {
        RecoveryResult recoveryResult;
        synchronized (completionMonitor) {
            if (response.isDone()) {
                return;
            }
            recoveryResult = restoreRetry(intake);
            response.completeExceptionally(exception);
        }
        compensateNewImage(intake, recoveryResult);
    }

    private void recoverAndCompensate(CertificationRetryIntake intake) {
        compensateNewImage(intake, restoreRetry(intake));
    }

    private RecoveryResult restoreRetry(CertificationRetryIntake intake) {
        try {
            return transactionService.restoreRetryIfProcessing(intake)
                    ? RecoveryResult.RESTORED
                    : RecoveryResult.NOT_PROCESSING;
        } catch (RuntimeException recoveryException) {
            log.error(
                    "Failed to restore retryable certification. certificationId={}",
                    intake.command().certificationId(),
                    recoveryException
            );
            return RecoveryResult.FAILED;
        }
    }

    private void compensateNewImage(
            CertificationRetryIntake intake,
            RecoveryResult recoveryResult
    ) {
        if (recoveryResult == RecoveryResult.RESTORED) {
            compensateImage(intake.command().imageKey());
        }
    }

    private void deletePreviousImage(CertificationRetryIntake intake) {
        String previousImageKey = intake.snapshot().imageKey();
        if (!Objects.equals(previousImageKey, intake.command().imageKey())) {
            compensateImage(previousImageKey);
        }
    }

    private void compensateImage(String imageKey) {
        try {
            imageStorage.delete(imageKey);
        } catch (RuntimeException compensationException) {
            log.error(
                    "Failed to compensate certification retry image. key={}",
                    imageKey,
                    compensationException
            );
        }
    }

    private CertificationException overloaded() {
        return new CertificationException(
                JUDGEMENT_OVERLOADED,
                CertificationErrorDetail.type("JUDGEMENT_OVERLOADED")
        );
    }

    private enum RecoveryResult {
        RESTORED,
        NOT_PROCESSING,
        FAILED
    }
}
