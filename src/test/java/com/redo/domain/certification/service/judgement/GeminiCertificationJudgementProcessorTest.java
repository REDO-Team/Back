package com.redo.domain.certification.service.judgement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.domain.certification.dto.CertificationJudgementCommand;
import com.redo.domain.certification.dto.CertificationJudgementContext;
import com.redo.domain.certification.dto.CertificationJudgementPreparation;
import com.redo.domain.certification.dto.CertificationVlmResult;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.service.CertificationTransactionService;
import com.redo.domain.certification.service.image.CertificationImageContent;
import com.redo.domain.certification.service.image.CertificationImageReader;
import com.redo.domain.recycleGuide.dto.ActiveRecycleJudgementTemplate;
import com.redo.domain.recycleGuide.entity.RecycleGuide;
import com.redo.domain.recycleGuide.repository.RecycleGuideRepository;
import com.redo.global.ai.gemini.client.GeminiClient;
import com.redo.global.ai.gemini.dto.GeminiRequest;
import com.redo.global.ai.gemini.dto.GeminiResponse;
import com.redo.global.ai.gemini.exception.GeminiException;
import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiCertificationJudgementProcessorTest {

    private static final String IMAGE_KEY = "certifications/42/image.jpg";

    @Mock
    private GeminiClient geminiClient;
    @Mock
    private CertificationImageReader imageReader;
    @Mock
    private RecycleGuideRepository recycleGuideRepository;
    @Mock
    private CertificationTransactionService transactionService;

    private GeminiCertificationJudgementProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new GeminiCertificationJudgementProcessor(
                geminiClient,
                imageReader,
                recycleGuideRepository,
                transactionService,
                new ObjectMapper()
        );
        when(imageReader.read(IMAGE_KEY))
                .thenReturn(new CertificationImageContent(
                        "image/jpeg",
                        new byte[]{1, 2, 3}
                ));
    }

    @Test
    void classifiesGeneralImageThenReturnsPersistedPassResult() {
        CertificationJudgementCommand command = command(CertificationSource.GENERAL, null);
        RecycleGuide guide = org.mockito.Mockito.mock(RecycleGuide.class);
        when(guide.getId()).thenReturn(12L);
        when(guide.getName()).thenReturn("투명 페트병");
        when(recycleGuideRepository.findAll()).thenReturn(List.of(guide));
        when(geminiClient.generate(any(GeminiRequest.class)))
                .thenReturn(
                        response("{\"recycleGuideName\":\"투명 페트병\"}"),
                        response("""
                                {
                                  "result": "PASS",
                                  "reason": "분리배출 기준을 충족했습니다.",
                                  "retryGuide": []
                                }
                                """)
                );
        CertificationJudgementContext context = context();
        when(transactionService.prepareJudgement(command, 12L))
                .thenReturn(CertificationJudgementPreparation.ready(context));
        CertificationCreateResponseDTO expected = passedResponse();
        when(transactionService.completeJudgement(
                any(CertificationJudgementContext.class),
                any(CertificationVlmResult.class)
        )).thenReturn(expected);

        CertificationCreateResponseDTO result = processor.process(command);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<GeminiRequest> requestCaptor =
                ArgumentCaptor.forClass(GeminiRequest.class);
        verify(geminiClient, times(2)).generate(requestCaptor.capture());
        List<GeminiRequest> requests = requestCaptor.getAllValues();
        assertThat(requests.get(0).jsonResponse()).isTrue();
        assertThat(requests.get(0).userPrompt()).contains("투명 페트병");
        assertThat(requests.get(0).media().get(0).mimeType()).isEqualTo("image/jpeg");
        assertThat(requests.get(1).responseSchema()).contains("PASS", "FAIL");

        ArgumentCaptor<CertificationVlmResult> resultCaptor =
                ArgumentCaptor.forClass(CertificationVlmResult.class);
        verify(transactionService).completeJudgement(
                org.mockito.ArgumentMatchers.eq(context),
                resultCaptor.capture()
        );
        assertThat(resultCaptor.getValue().result().name()).isEqualTo("PASS");
        assertThat(resultCaptor.getValue().rawResponseJson()).contains("\"PASS\"");
    }

    @Test
    void skipsGeminiJudgementWhenGuideWasAlreadyPassedToday() {
        CertificationJudgementCommand command =
                command(CertificationSource.AFTER_SEARCH, 12L);
        CertificationCreateResponseDTO duplicate = new CertificationCreateResponseDTO(
                101L,
                CertificationStatus.FAILED,
                com.redo.domain.certification.enums.CertificationFailureType
                        .DUPLICATE_GUIDE_TODAY,
                12L,
                "투명 페트병",
                "플라스틱",
                0,
                "오늘 이미 인증한 동일 가이드입니다.",
                List.of("다른 품목을 촬영해 주세요."),
                false,
                null,
                LocalDateTime.of(2026, 7, 31, 14, 3)
        );
        when(transactionService.prepareJudgement(command, 12L))
                .thenReturn(CertificationJudgementPreparation.completed(duplicate));

        CertificationCreateResponseDTO result = processor.process(command);

        assertThat(result).isSameAs(duplicate);
        verify(geminiClient, never()).generate(any());
        verify(transactionService, never()).completeJudgement(any(), any());
    }

    @Test
    void rejectsInvalidStructuredJudgementAsSystemError() {
        CertificationJudgementCommand command =
                command(CertificationSource.AFTER_SEARCH, 12L);
        when(transactionService.prepareJudgement(command, 12L))
                .thenReturn(CertificationJudgementPreparation.ready(context()));
        when(geminiClient.generate(any(GeminiRequest.class)))
                .thenReturn(response("{\"result\":\"PASS\"}"));

        assertThatThrownBy(() -> processor.process(command))
                .isInstanceOf(GeminiException.class)
                .extracting("errorCode")
                .isEqualTo(GeminiErrorCode.EMPTY_OR_INVALID_RESPONSE);

        verify(transactionService, never()).completeJudgement(any(), any());
    }

    private CertificationJudgementCommand command(
            CertificationSource source,
            Long recycleGuideId
    ) {
        return new CertificationJudgementCommand(
                101L,
                42L,
                source,
                IMAGE_KEY,
                recycleGuideId
        );
    }

    private CertificationJudgementContext context() {
        return new CertificationJudgementContext(
                101L,
                42L,
                CertificationSource.AFTER_SEARCH,
                12L,
                "투명 페트병",
                "플라스틱",
                100,
                new ActiveRecycleJudgementTemplate(
                        7L,
                        12L,
                        1,
                        "라벨을 제거했는지 확인",
                        "{\"clean\":true}",
                        "{\"contaminated\":true}",
                        "세척 후 다시 촬영"
                )
        );
    }

    private CertificationCreateResponseDTO passedResponse() {
        return new CertificationCreateResponseDTO(
                101L,
                CertificationStatus.PASSED,
                null,
                12L,
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

    private GeminiResponse response(String content) {
        return new GeminiResponse(content, "gemini-test", null, null, null, "STOP");
    }
}
