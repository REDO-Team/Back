package com.redo.domain.certification.service;

import com.redo.domain.certification.config.CertificationJudgementProperties;
import com.redo.domain.certification.dto.CertificationJudgementCommand;
import com.redo.domain.certification.dto.req.CertificationCreateRequestDTO;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
import com.redo.domain.certification.dto.res.CertificationErrorDetail;
import com.redo.domain.certification.enums.CertificationSource;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

import static com.redo.domain.certification.config.CertificationJudgementConfig.JUDGEMENT_EXECUTOR;
import static com.redo.domain.certification.config.CertificationJudgementConfig.TIMEOUT_SCHEDULER;
import static com.redo.domain.certification.exception.code.CertificationErrorCode.INVALID_SOURCE_GUIDE_CONTRACT;
import static com.redo.domain.certification.exception.code.CertificationErrorCode.JUDGEMENT_OVERLOADED;
import static com.redo.global.ai.gemini.exception.code.GeminiErrorCode.TIMEOUT;

@Slf4j
@Service
public class CertificationCreateService {

    private final CertificationTransactionService transactionService;
    private final CertificationImageStorage imageStorage;
    private final CertificationJudgementProcessor judgementProcessor;
    private final ThreadPoolTaskExecutor judgementExecutor;
    private final TaskScheduler timeoutScheduler;
    private final CertificationJudgementProperties properties;

    public CertificationCreateService(
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

    public CompletableFuture<CertificationCreateResponseDTO> create(
            Long userId,
            CertificationCreateRequestDTO request
    ) {
        CertificationSource source = parseAndValidateSourceGuide(request);
        if (source == CertificationSource.AFTER_SEARCH) {
            transactionService.validateAfterSearchPrerequisites(
                    request.recycleGuideId()
            );
        }

        String imageKey = imageStorage.upload(userId, request.image());
        CertificationJudgementCommand command;
        try {
            command = transactionService.createProcessing(
                    userId,
                    source,
                    request.recycleGuideId(),
                    imageKey
            );
        } catch (RuntimeException exception) {
            compensateImage(imageKey);
            throw exception;
        }

        CompletableFuture<CertificationCreateResponseDTO> response =
                new CompletableFuture<>();
        Object completionMonitor = new Object();
        ScheduledFuture<?> timeoutTask;
        try {
            timeoutTask = timeoutScheduler.schedule(
                    () -> timeout(command, response, completionMonitor),
                    Instant.now().plus(properties.timeout())
            );
        } catch (TaskRejectedException exception) {
            cleanup(command);
            throw overloaded();
        }
        response.whenComplete((ignored, throwable) -> timeoutTask.cancel(false));

        try {
            judgementExecutor.execute(() -> process(
                    command,
                    response,
                    completionMonitor
            ));
        } catch (TaskRejectedException exception) {
            timeoutTask.cancel(false);
            cleanup(command);
            throw overloaded();
        }
        return response;
    }

    private void process(
            CertificationJudgementCommand command,
            CompletableFuture<CertificationCreateResponseDTO> response,
            Object completionMonitor
    ) {
        CertificationCreateResponseDTO result;
        try {
            result = judgementProcessor.process(command);
        } catch (Exception exception) {
            fail(command, response, completionMonitor, exception);
            return;
        } catch (Error error) {
            log.error(
                    "Fatal error during certification judgement. certificationId={}",
                    command.certificationId(),
                    error
            );
            fail(command, response, completionMonitor, error);
            throw error;
        }

        synchronized (completionMonitor) {
            response.complete(result);
        }
    }

    private void timeout(
            CertificationJudgementCommand command,
            CompletableFuture<CertificationCreateResponseDTO> response,
            Object completionMonitor
    ) {
        CleanupResult cleanupResult;
        synchronized (completionMonitor) {
            if (response.isDone()) {
                return;
            }

            cleanupResult = deleteProcessing(command);
            if (cleanupResult == CleanupResult.NOT_PROCESSING) {
                return;
            }
            response.completeExceptionally(new GeminiException(TIMEOUT));
        }
        compensateDeletedImage(command, cleanupResult);
    }

    private CertificationSource parseAndValidateSourceGuide(
            CertificationCreateRequestDTO request
    ) {
        if (request == null) {
            throw invalidContract();
        }

        CertificationSource source;
        try {
            source = CertificationSource.valueOf(request.certificationSource());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalidContract();
        }

        boolean invalidGeneral =
                source == CertificationSource.GENERAL && request.recycleGuideId() != null;
        boolean invalidAfterSearch =
                source == CertificationSource.AFTER_SEARCH && request.recycleGuideId() == null;
        if (invalidGeneral || invalidAfterSearch) {
            throw invalidContract();
        }
        return source;
    }

    private CertificationException invalidContract() {
        return new CertificationException(
                INVALID_SOURCE_GUIDE_CONTRACT,
                CertificationErrorDetail.type("INVALID_SOURCE_GUIDE_CONTRACT")
        );
    }

    private CertificationException overloaded() {
        return new CertificationException(
                JUDGEMENT_OVERLOADED,
                CertificationErrorDetail.type("JUDGEMENT_OVERLOADED")
        );
    }

    private void cleanup(CertificationJudgementCommand command) {
        compensateDeletedImage(command, deleteProcessing(command));
    }

    private void fail(
            CertificationJudgementCommand command,
            CompletableFuture<CertificationCreateResponseDTO> response,
            Object completionMonitor,
            Throwable exception
    ) {
        CleanupResult cleanupResult;
        synchronized (completionMonitor) {
            if (response.isDone()) {
                return;
            }
            cleanupResult = deleteProcessing(command);
            response.completeExceptionally(exception);
        }
        compensateDeletedImage(command, cleanupResult);
    }

    private CleanupResult deleteProcessing(CertificationJudgementCommand command) {
        try {
            return transactionService.deleteIfProcessing(command)
                    ? CleanupResult.DELETED
                    : CleanupResult.NOT_PROCESSING;
        } catch (RuntimeException cleanupException) {
            log.error(
                    "Failed to clean up processing certification. certificationId={}",
                    command.certificationId(),
                    cleanupException
            );
            return CleanupResult.FAILED;
        }
    }

    private void compensateDeletedImage(
            CertificationJudgementCommand command,
            CleanupResult cleanupResult
    ) {
        if (cleanupResult == CleanupResult.DELETED) {
            compensateImage(command.imageKey());
        }
    }

    private void compensateImage(String imageKey) {
        try {
            imageStorage.delete(imageKey);
        } catch (RuntimeException compensationException) {
            log.error(
                    "Failed to compensate certification image. key={}",
                    imageKey,
                    compensationException
            );
        }
    }

    private enum CleanupResult {
        DELETED,
        NOT_PROCESSING,
        FAILED
    }
}
