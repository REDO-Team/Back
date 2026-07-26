package com.redo.domain.contribution.dto.res;

import com.redo.domain.contribution.enums.ContributionType;

import java.time.LocalDateTime;
import java.util.List;

public record OverallContributionResponseDTO(
        long totalParticipantCount,
        String summaryMessage,
        List<FeedDTO> feeds,
        Long nextCursor,
        boolean hasNext
) {

    public record FeedDTO(
            Long feedId,
            Long userId,
            String nickname,
            String profileImageUrl,
            String message,
            String highlightText,
            ContributionType eventType,
            String targetName,
            Integer remainingCount,
            LocalDateTime createdAt
    ) {
    }
}
