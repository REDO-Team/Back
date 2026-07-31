package com.redo.domain.certification.service;

import com.redo.domain.certification.config.CertificationJudgementProperties;
import com.redo.domain.certification.dto.CertificationJudgementCommand;
import com.redo.domain.certification.dto.req.CertificationCreateRequestDTO;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificationCreateServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long GUIDE_ID = 12L;
    private static final String IMAGE_KEY = "certifications/42/image.jpg";

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
    private CertificationCreateService service;

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

        service = new CertificationCreateService(
                transactionService,
                imageStorage,
                judgementProcessor,
                executor,
                timeoutScheduler,
                new CertificationJudgementProperties(
                        Duration.ofSeconds(5),
                        1,
                        1,
                        1
                )
        );
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void returnsTerminalResultFromDedicatedExecutor() {
        CertificationJudgementCommand command = command(
                CertificationSource.AFTER_SEARCH,
                GUIDE_ID
        );
        CertificationCreateResponseDTO passed = passedResponse();
        when(imageStorage.upload(eq(USER_ID), any())).thenReturn(IMAGE_KEY);
        when(transactionService.createProcessing(
                USER_ID,
                CertificationSource.AFTER_SEARCH,
                GUIDE_ID,
                IMAGE_KEY
        )).thenReturn(command);
        when(judgementProcessor.process(command)).thenReturn(passed);

        CertificationCreateResponseDTO response = service.create(
                USER_ID,
                request("AFTER_SEARCH", GUIDE_ID)
        ).join();

        assertThat(response.status()).isEqualTo(CertificationStatus.PASSED);
        assertThat(response.earnedPoint()).isEqualTo(100);
        verify(transactionService).validateAfterSearchPrerequisites(GUIDE_ID);
        verify(timeoutTask).cancel(false);
    }

    @Test
    void rejectsInvalidSourceGuideContractBeforeUpload() {
        assertThatThrownBy(() -> service.create(
                USER_ID,
                request("GENERAL", GUIDE_ID)
        ))
                .isInstanceOf(CertificationException.class)
                .extracting("errorCode")
                .isEqualTo(CertificationErrorCode.INVALID_SOURCE_GUIDE_CONTRACT);

        verifyNoInteractions(imageStorage, transactionService, judgementProcessor);
    }

    @Test
    void compensatesImageWhenIntakeTransactionFails() {
        RuntimeException intakeFailure = new IllegalStateException("db failed");
        when(imageStorage.upload(eq(USER_ID), any())).thenReturn(IMAGE_KEY);
        when(transactionService.createProcessing(
                USER_ID,
                CertificationSource.GENERAL,
                null,
                IMAGE_KEY
        )).thenThrow(intakeFailure);

        assertThatThrownBy(() -> service.create(
                USER_ID,
                request("GENERAL", null)
        )).isSameAs(intakeFailure);

        verify(imageStorage).delete(IMAGE_KEY);
        verifyNoInteractions(judgementProcessor);
    }

    @Test
    void cleansProcessingRowAndImageOnSystemFailure() {
        CertificationJudgementCommand command = command(
                CertificationSource.GENERAL,
                null
        );
        RuntimeException providerFailure = new IllegalStateException("provider failed");
        when(imageStorage.upload(eq(USER_ID), any())).thenReturn(IMAGE_KEY);
        when(transactionService.createProcessing(
                USER_ID,
                CertificationSource.GENERAL,
                null,
                IMAGE_KEY
        )).thenReturn(command);
        when(judgementProcessor.process(command)).thenThrow(providerFailure);
        when(transactionService.deleteIfProcessing(command)).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                USER_ID,
                request("GENERAL", null)
        ).join())
                .isInstanceOf(CompletionException.class)
                .hasCause(providerFailure);

        verify(transactionService).deleteIfProcessing(command);
        verify(imageStorage).delete(IMAGE_KEY);
    }

    @Test
    void doesNotDeleteImageWhenCertificationAlreadyReachedTerminalState() {
        CertificationJudgementCommand command = command(
                CertificationSource.GENERAL,
                null
        );
        when(imageStorage.upload(eq(USER_ID), any())).thenReturn(IMAGE_KEY);
        when(transactionService.createProcessing(
                USER_ID,
                CertificationSource.GENERAL,
                null,
                IMAGE_KEY
        )).thenReturn(command);
        when(judgementProcessor.process(command))
                .thenThrow(new IllegalStateException("late response"));
        when(transactionService.deleteIfProcessing(command)).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                USER_ID,
                request("GENERAL", null)
        ).join()).isInstanceOf(CompletionException.class);

        verify(imageStorage, never()).delete(IMAGE_KEY);
    }

    @Test
    void timesOutAndCleansProcessingStateWithoutSavingVlmFailure() throws Exception {
        CertificationJudgementCommand command = command(
                CertificationSource.GENERAL,
                null
        );
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        when(imageStorage.upload(eq(USER_ID), any())).thenReturn(IMAGE_KEY);
        when(transactionService.createProcessing(
                USER_ID,
                CertificationSource.GENERAL,
                null,
                IMAGE_KEY
        )).thenReturn(command);
        when(transactionService.deleteIfProcessing(command)).thenReturn(true);
        when(judgementProcessor.process(command)).thenAnswer(invocation -> {
            workerStarted.countDown();
            releaseWorker.await(3, TimeUnit.SECONDS);
            return passedResponse();
        });

        CompletableFuture<CertificationCreateResponseDTO> response = service.create(
                USER_ID,
                request("GENERAL", null)
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
            verify(transactionService).deleteIfProcessing(command);
            verify(imageStorage).delete(IMAGE_KEY);
        } finally {
            releaseWorker.countDown();
        }
    }

    @Test
    void rejectsOverloadedSchedulerAndCompensatesIntake() {
        CertificationJudgementCommand command = command(
                CertificationSource.GENERAL,
                null
        );
        when(imageStorage.upload(eq(USER_ID), any())).thenReturn(IMAGE_KEY);
        when(transactionService.createProcessing(
                USER_ID,
                CertificationSource.GENERAL,
                null,
                IMAGE_KEY
        )).thenReturn(command);
        when(timeoutScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .thenThrow(new TaskRejectedException("scheduler unavailable"));
        when(transactionService.deleteIfProcessing(command)).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                USER_ID,
                request("GENERAL", null)
        ))
                .isInstanceOf(CertificationException.class)
                .extracting("errorCode")
                .isEqualTo(CertificationErrorCode.JUDGEMENT_OVERLOADED);

        verify(transactionService).deleteIfProcessing(command);
        verify(imageStorage).delete(IMAGE_KEY);
        verifyNoInteractions(judgementProcessor);
    }

    private CertificationJudgementCommand command(
            CertificationSource source,
            Long recycleGuideId
    ) {
        return new CertificationJudgementCommand(
                101L,
                USER_ID,
                source,
                IMAGE_KEY,
                recycleGuideId
        );
    }

    private CertificationCreateRequestDTO request(
            String source,
            Long recycleGuideId
    ) {
        return new CertificationCreateRequestDTO(
                new MockMultipartFile(
                        "image",
                        "can.jpg",
                        "image/jpeg",
                        "image".getBytes()
                ),
                source,
                recycleGuideId
        );
    }

    private CertificationCreateResponseDTO passedResponse() {
        return new CertificationCreateResponseDTO(
                101L,
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
                LocalDateTime.of(2026, 7, 31, 14, 3)
        );
    }
}
