package com.redo.domain.recycleGuide.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.domain.recycleGuide.dto.RecycleGuideResponseDTO.ImageSearchResultDTO;
import com.redo.domain.recycleGuide.dto.RecycleGuideResponseDTO.GuideDetailDTO;
import com.redo.domain.recycleGuide.repository.RecycleGuideRepository;
import com.redo.global.ai.gemini.client.GeminiClient;
import com.redo.global.ai.gemini.dto.GeminiMedia;
import com.redo.global.ai.gemini.dto.GeminiRequest;
import com.redo.global.ai.gemini.dto.GeminiResponse;
import com.redo.global.apiPayload.exception.GeneralException;
import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecycleGuideAiService {

    private final GeminiClient geminiClient;
    private final RecycleGuideRepository recycleGuideRepository;
    private final RecycleGuideService recycleGuideService;
    private final ObjectMapper objectMapper;

    public ImageSearchResultDTO searchGuideByImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new GeneralException(GeminiErrorCode.EMPTY_FILE);
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new GeneralException(GeminiErrorCode.INVALID_IMAGE_TYPE);
        }

        if (image.getSize() > 10 * 1024 * 1024) {
            throw new GeneralException(GeminiErrorCode.FILE_SIZE_EXCEEDED);
        }

        List<String> validNames = recycleGuideRepository.findAllNames();

        GeminiMedia media;
        try {
            media = new GeminiMedia(image.getContentType(), image.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("이미지 데이터를 읽는 중 오류가 발생했습니다.", e);
        }

        String jsonSchema = """
                {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string", "description": "The exact matched item name from the list, or NOT_FOUND" },
                    "reason": { "type": "string", "description": "The reasoning for choosing this item" }
                  },
                  "required": ["name", "reason"]
                }
                """;

        String userPrompt = "Choose the ONE most appropriate item name from the provided list that matches the garbage in the image. If none match or it's difficult to tell, you MUST answer with 'NOT_FOUND'.\\nList: " + validNames;

        GeminiRequest request = new GeminiRequest(
                "You are an expert in recycling and waste sorting. Based on the provided image and list of item names, identify the exact item name in JSON format.",
                userPrompt,
                List.of(media),
                true,
                jsonSchema,
                null,
                0.1,
                300
        );

        GeminiResponse response = geminiClient.generate(request);

        String identifiedName;
        String reason;
        try {
            JsonNode resultNode = objectMapper.readTree(response.content());
            identifiedName = resultNode.path("name").asText();
            reason = resultNode.path("reason").asText();
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", response.content(), e);
            return ImageSearchResultDTO.builder()
                    .isIdentified(false)
                    .reason("AI 응답을 분석하는 중 오류가 발생했습니다.")
                    .guideDetail(null)
                    .build();
        }

        // 'NOT_FOUND' 거나 결과가 우리 DB에 없는 이름일 경우 식별 실패로 간주
        if ("NOT_FOUND".equals(identifiedName) || identifiedName.isBlank() || !validNames.contains(identifiedName)) {
            return ImageSearchResultDTO.builder()
                    .isIdentified(false)
                    .reason(reason)
                    .guideDetail(null)
                    .build();
        }

        GuideDetailDTO guideDetail = recycleGuideService.getGuideByName(identifiedName);

        return ImageSearchResultDTO.builder()
                .isIdentified(true)
                .reason(reason)
                .guideDetail(guideDetail)
                .build();
    }
}
