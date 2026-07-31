package com.redo.domain.certification.service.judgement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.domain.certification.dto.CertificationJudgementCommand;
import com.redo.domain.certification.dto.CertificationJudgementContext;
import com.redo.domain.certification.dto.CertificationJudgementPreparation;
import com.redo.domain.certification.dto.CertificationVlmResult;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
import com.redo.domain.certification.dto.res.CertificationErrorDetail;
import com.redo.domain.certification.enums.AiJudgementResult;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.exception.CertificationException;
import com.redo.domain.certification.service.CertificationTransactionService;
import com.redo.domain.certification.service.image.CertificationImageContent;
import com.redo.domain.certification.service.image.CertificationImageReader;
import com.redo.domain.recycleGuide.entity.RecycleGuide;
import com.redo.domain.recycleGuide.repository.RecycleGuideRepository;
import com.redo.global.ai.gemini.client.GeminiClient;
import com.redo.global.ai.gemini.dto.GeminiMedia;
import com.redo.global.ai.gemini.dto.GeminiRequest;
import com.redo.global.ai.gemini.dto.GeminiResponse;
import com.redo.global.ai.gemini.exception.GeminiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.redo.domain.certification.exception.code.CertificationErrorCode.RECYCLE_GUIDE_NOT_FOUND;
import static com.redo.global.ai.gemini.exception.code.GeminiErrorCode.EMPTY_OR_INVALID_RESPONSE;

@Component
@RequiredArgsConstructor
public class GeminiCertificationJudgementProcessor
        implements CertificationJudgementProcessor {

    private static final String CLASSIFICATION_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "recycleGuideName": {"type": "string"}
              },
              "required": ["recycleGuideName"]
            }
            """;
    private static final String JUDGEMENT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "result": {"type": "string", "enum": ["PASS", "FAIL"]},
                "reason": {"type": "string"},
                "retryGuide": {
                  "type": "array",
                  "items": {"type": "string"}
                }
              },
              "required": ["result", "reason", "retryGuide"]
            }
            """;
    private static final String SYSTEM_INSTRUCTION = """
            당신은 분리배출 인증 사진을 판정하는 시스템입니다.
            제공된 조건과 사진만 사용하고, 지정된 JSON 스키마로만 응답하세요.
            """;

    private final GeminiClient geminiClient;
    private final CertificationImageReader imageReader;
    private final RecycleGuideRepository recycleGuideRepository;
    private final CertificationTransactionService transactionService;
    private final ObjectMapper objectMapper;

    @Override
    public CertificationCreateResponseDTO process(
            CertificationJudgementCommand command
    ) {
        CertificationImageContent image = imageReader.read(command.imageKey());
        Long recycleGuideId = command.source() == CertificationSource.GENERAL
                ? classifyRecycleGuide(image)
                : command.recycleGuideId();

        CertificationJudgementPreparation preparation =
                transactionService.prepareJudgement(command, recycleGuideId);
        if (preparation.isCompleted()) {
            return preparation.completedResponse();
        }

        CertificationJudgementContext context = preparation.context();
        CertificationVlmResult result = judge(context, image);
        return transactionService.completeJudgement(context, result);
    }

    private Long classifyRecycleGuide(CertificationImageContent image) {
        List<GuideCandidate> candidates = recycleGuideRepository.findAll().stream()
                .map(guide -> new GuideCandidate(guide.getId(), guide.getName()))
                .toList();
        if (candidates.isEmpty()) {
            throw classificationMappingFailed();
        }

        GeminiResponse response = geminiClient.generate(new GeminiRequest(
                SYSTEM_INSTRUCTION,
                """
                        사진 속 폐기물 품목을 분류하세요.
                        recycleGuideName은 반드시 다음 후보 중 하나를 그대로 사용하세요.
                        후보: %s
                        """.formatted(toJson(candidates.stream()
                        .map(GuideCandidate::name)
                        .toList())),
                List.of(toMedia(image)),
                true,
                CLASSIFICATION_SCHEMA,
                null,
                0.0,
                null
        ));
        String classifiedName = requiredText(
                parseObject(response.content()).get("recycleGuideName")
        );
        return candidates.stream()
                .filter(candidate -> candidate.name().equals(classifiedName))
                .map(GuideCandidate::id)
                .findFirst()
                .orElseThrow(this::classificationMappingFailed);
    }

    private CertificationVlmResult judge(
            CertificationJudgementContext context,
            CertificationImageContent image
    ) {
        GeminiResponse response = geminiClient.generate(new GeminiRequest(
                SYSTEM_INSTRUCTION,
                """
                        품목: %s
                        판정 지침: %s
                        통과 조건(JSON): %s
                        실패 조건(JSON): %s
                        재촬영 안내 기준: %s

                        사진이 통과 조건을 만족하면 PASS, 아니면 FAIL로 판정하세요.
                        reason에는 사용자에게 보여 줄 간결한 근거를 작성하세요.
                        FAIL이면 retryGuide에 구체적인 재촬영 행동을 하나 이상 작성하세요.
                        """.formatted(
                        context.itemName(),
                        nullToEmpty(context.template().promptTemplate()),
                        nullToEmpty(context.template().passConditionsJson()),
                        nullToEmpty(context.template().failConditionsJson()),
                        nullToEmpty(context.template().retryGuideTemplate())
                ),
                List.of(toMedia(image)),
                true,
                JUDGEMENT_SCHEMA,
                null,
                0.0,
                null
        ));
        return parseJudgement(response.content());
    }

    private CertificationVlmResult parseJudgement(String rawJson) {
        JsonNode root = parseObject(rawJson);
        String resultText = requiredText(root.get("result"));
        String reason = requiredText(root.get("reason"));
        JsonNode retryGuideNode = root.get("retryGuide");
        if (retryGuideNode == null || !retryGuideNode.isArray()) {
            throw invalidResponse();
        }

        List<String> retryGuide = new ArrayList<>();
        retryGuideNode.forEach(value -> retryGuide.add(requiredText(value)));

        AiJudgementResult result;
        try {
            result = AiJudgementResult.valueOf(resultText);
        } catch (IllegalArgumentException exception) {
            throw invalidResponse();
        }
        if (result == AiJudgementResult.FAIL && retryGuide.isEmpty()) {
            throw invalidResponse();
        }
        return new CertificationVlmResult(result, reason, retryGuide, rawJson);
    }

    private JsonNode parseObject(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (root == null || !root.isObject()) {
                throw invalidResponse();
            }
            return root;
        } catch (JsonProcessingException exception) {
            throw new GeminiException(EMPTY_OR_INVALID_RESPONSE, exception);
        }
    }

    private String requiredText(JsonNode value) {
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw invalidResponse();
        }
        return value.asText().trim();
    }

    private GeminiMedia toMedia(CertificationImageContent image) {
        return new GeminiMedia(image.mimeType(), image.bytes());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Gemini prompt value", exception);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private GeminiException invalidResponse() {
        return new GeminiException(EMPTY_OR_INVALID_RESPONSE);
    }

    private CertificationException classificationMappingFailed() {
        return new CertificationException(
                RECYCLE_GUIDE_NOT_FOUND,
                CertificationErrorDetail.type("GENERAL_CLASSIFICATION_NOT_MAPPED")
        );
    }

    private record GuideCandidate(Long id, String name) {
    }
}
