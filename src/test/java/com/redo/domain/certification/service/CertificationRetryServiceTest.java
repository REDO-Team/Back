package com.redo.domain.certification.service;

import com.redo.domain.certification.config.CertificationJudgementProperties;
import com.redo.domain.certification.dto.CertificationJudgementCommand;
import com.redo.domain.certification.dto.CertificationRetryIntake;
import com.redo.domain.certification.dto.CertificationRetrySnapshot;
import com.redo.domain.certification.dto.req.CertificationRetryRequestDTO;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
import com.redo.domain.certification.dto.res.CertificationRetryResponseDTO;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.exception.CertificationException;
import com.redo.domain.certification.exception.code.CertificationErrorCode;
import com.redo.domain.certification.service.image.CertificationImageStorage;
import com.redo.domain.certification.service.judgement.CertificationJudgementProcessor;
import com.redo.global.ai.gemini.exception.GeminiException;
import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificationRetryServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long CERTIFICATION_ID = 101L;
    private static final Long GUIDE_ID = 12L;
    private static final String PREVIOUS_IMAGE_KEY =
            "certifications/42/previous.jpg";
    private static final String RETRY_IMAGE_KEY =
            "certifications/42/retry.jpg";

    @Mock
    private CertificationTransactionService transactionService;
    @Mock
    private CertificationImageStorage imageStorage;
    @Mock
    private CertificationJudgementProcessor judgementProcessor;
    @Mock
    private TaskScheduler timeoutScheduler;
    @Mock
    private ScheduledFuture<?> timeoutTask;

    private ThreadPoolTaskExecutor executor;
    private CertificationRetryService service;

    @BeforeEach
    void setUp() {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.initialize();
        lenient().doReturn(timeoutTask)
                .when(timeoutScheduler)
                .schedule(any(Runnable.class), any(Instant.class));

        service = retryService(executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void returnsTerminalResultAndDeletesPreviousImage() {
        CertificationRetryIntake intake = intake(CertificationSource.GENERAL);
        when(imageStorage.upload(eq(USER_ID), any())).thenReturn(RETRY_IMAGE_KEY);
        when(transactionService.startRetry(
                USER_ID,
                CERTIFICATION_ID,
                RETRY_IMAGE_KEY
        )).thenReturn(intake);
        when(judgementProcessor.process(intake.command()))
                .thenReturn(passedResponse());

        CertificationRetryResponseDTO response = service.retry(
                USER_ID,
                CERTIFICATION_ID,
                request()
        ).join();

        assertThat(response.status()).isEqualTo(CertificationStatus.PASSED);
        assertThat(response.attemptCount()).isEqualTo(2);
        assertThat(intake.command().mode().classificationRequired()).isFalse();
        assertThat(intake.command().recycleGuideId()).isEqualTo(GUIDE_ID);
        verify(transactionService).validateRetryPrerequisites(
                USER_ID,
                CERTIFICATION_ID
        );
        verify(imageStorage, timeout(1000)).delete(PREVIOUS_IMAGE_KEY);
        verify(imageStorage, never()).delete(RETRY_IMAGE_KEY);
        verify(timeoutTask).cancel(false);
    }

    @Test
    void restoresPreviousFailureAndDeletesNewImageOnSystemFailure() {
        CertificationRetryIntake intake = intake(CertificationSource.AFTER_SEARCH);
        RuntimeException providerFailure = new IllegalStateException("provider failed");
        when(imageStorage.upload(eq(USER_ID), any())).thenReturn(RETRY_IMAGE_KEY);
        when(transactionService.startRetry(
                USER_ID,
                CERTIFICATION_ID,
                RETRY_IMAGE_KEY
        )).thenReturn(intake);
        when(judgementProcessor.process(intake.command())).thenThrow(providerFailure);
        when(transactionService.restoreRetryIfProcessing(intake)).thenReturn(true);

        assertThatThrownBy(() -> service.retry(
                USER_ID,
                CERTIFICATION_ID,
                request()
        ).join())
                .isInstanceOf(CompletionException.class)
                .hasCause(providerFailure);

        verify(transactionService).restoreRetryIfProcessing(intake);
        verify(imageStorage, timeout(1000)).delete(RETRY_IMAGE_KEY);
        verify(imageStorage, after(500).never()).delete(PREVIOUS_IMAGE_KEY);
    }

    @Test
    void timesOutAndRestoresRetryableFailure() throws Exception {
        CertificationRetryIntake intake = intake(CertificationSource.AFTER_SEARCH);
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        when(imageStorage.upload(eq(USER_ID), any())).thenReturn(RETRY_IMAGE_KEY);
        when(transactionService.startRetry(
                USER_ID,
                CERTIFICATION_ID,
                RETRY_IMAGE_KEY
        )).thenReturn(intake);
        when(transactionService.restoreRetryIfProcessing(intake)).thenReturn(true);
        when(judgementProcessor.process(intake.command())).thenAnswer(invocation -> {
            workerStarted.countDown();
            releaseWorker.await(3, TimeUnit.SECONDS);
            return passedResponse();
        });

        CompletableFuture<CertificationRetryResponseDTO> response = service.retry(
                USER_ID,
                CERTIFICATION_ID,
                request()
        );
        ArgumentCaptor<Runnable> timeoutCaptor =
                ArgumentCaptor.forClass(Runnable.class);
        verify(timeoutScheduler).schedule(timeoutCaptor.capture(), any(Instant.class));
        assertThat(workerStarted.await(1, TimeUnit.SECONDS)).isTrue();

        try {
            timeoutCaptor.getValue().run();

            assertThatThrownBy(response::join)
                    .isInstanceOf(CompletionException.class)
                    .cause()
                    .isInstanceOf(GeminiException.class)
                    .extracting("errorCode")
                    .isEqualTo(GeminiErrorCode.TIMEOUT);
            verify(transactionService).restoreRetryIfProcessing(intake);
            verify(imageStorage).delete(RETRY_IMAGE_KEY);
        } finally {
            releaseWorker.countDown();
        }
        verify(imageStorage, after(500).never()).delete(PREVIOUS_IMAGE_KEY);
    }

    @Test
    void returnsWorkerResultWhenTimeoutFindsTerminalState() throws Exception {
        CertificationRetryIntake intake = intake(CertificationSource.AFTER_SEARCH);
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        when(imageStorage.upload(eq(USER_ID), any())).thenReturn(RETRY_IMAGE_KEY);
        when(transactionService.startRetry(
                USER_ID,
                CERTIFICATION_ID,
                RETRY_IMAGE_KEY
        )).thenReturn(intake);
        when(transactionService.restoreRetryIfProcessing(intake)).thenReturn(false);
        when(judgementProcessor.process(intake.command())).thenAnswer(invocation -> {
            workerStarted.countDown();
            releaseWorker.await(3, TimeUnit.SECONDS);
            return passedResponse();
        });

        CompletableFuture<CertificationRetryResponseDTO> response = service.retry(
                USER_ID,
                CERTIFICATION_ID,
                request()
        );
        ArgumentCaptor<Runnable> timeoutCaptor =
                ArgumentCaptor.forClass(Runnable.class);
        verify(timeoutScheduler).schedule(timeoutCaptor.capture(), any(Instant.class));
        assertThat(workerStarted.await(1, TimeUnit.SECONDS)).isTrue();

        timeoutCaptor.getValue().run();
        assertThat(response).isNotDone();
        releaseWorker.countDown();

        assertThat(response.join().status()).isEqualTo(CertificationStatus.PASSED);
        verify(imageStorage, timeout(1000)).delete(PREVIOUS_IMAGE_KEY);
        verify(imageStorage, never()).delete(RETRY_IMAGE_KEY);
    }

    @Test
    void rejectsOverloadedExecutorAndRestoresIntake() {
        CertificationRetryIntake intake = intake(CertificationSource.AFTER_SEARCH);
        ThreadPoolTaskExecutor rejectedExecutor = mock(ThreadPoolTaskExecutor.class);
        CertificationRetryService rejectedService = retryService(rejectedExecutor);
        when(imageStorage.upload(eq(USER_ID), any())).thenReturn(RETRY_IMAGE_KEY);
        when(transactionService.startRetry(
                USER_ID,
                CERTIFICATION_ID,
                RETRY_IMAGE_KEY
        )).thenReturn(intake);
        when(transactionService.restoreRetryIfProcessing(intake)).thenReturn(true);
        doThrow(new TaskRejectedException("executor saturated"))
                .when(rejectedExecutor)
                .execute(any(Runnable.class));

        assertThatThrownBy(() -> rejectedService.retry(
                USER_ID,
                CERTIFICATION_ID,
                request()
        ))
                .isInstanceOf(CertificationException.class)
                .extracting("errorCode")
                .isEqualTo(CertificationErrorCode.JUDGEMENT_OVERLOADED);

        verify(timeoutTask).cancel(false);
        verify(transactionService).restoreRetryIfProcessing(intake);
        verify(imageStorage).delete(RETRY_IMAGE_KEY);
        verifyNoInteractions(judgementProcessor);
    }

    private CertificationRetryService retryService(
            ThreadPoolTaskExecutor taskExecutor
    ) {
        return new CertificationRetryService(
                transactionService,
                imageStorage,
                judgementProcessor,
                taskExecutor,
                timeoutScheduler,
                new CertificationJudgementProperties(
                        Duration.ofSeconds(5),
                        1,
                        1,
                        1,
                        4
                )
        );
    }

    private CertificationRetryIntake intake(CertificationSource source) {
        CertificationJudgementCommand command = CertificationJudgementCommand.retry(
                CERTIFICATION_ID,
                USER_ID,
                source,
                RETRY_IMAGE_KEY,
                GUIDE_ID
        );
        return new CertificationRetryIntake(
                command,
                new CertificationRetrySnapshot(
                        PREVIOUS_IMAGE_KEY,
                        LocalDateTime.of(2026, 7, 31, 14, 3),
                        1
                ),
                2
        );
    }

    private CertificationRetryRequestDTO request() {
        return new CertificationRetryRequestDTO(new MockMultipartFile(
                "image",
                "retry.jpg",
                "image/jpeg",
                "image".getBytes()
        ));
    }

    private CertificationCreateResponseDTO passedResponse() {
        return new CertificationCreateResponseDTO(
                CERTIFICATION_ID,
                CertificationStatus.PASSED,
                null,
                GUIDE_ID,
                "투명 페트병",
                "플라스틱",
                100,
                null,
                List.of(),
                false,
                null,
                LocalDateTime.of(2026, 7, 31, 14, 8)
        );
    }
}
