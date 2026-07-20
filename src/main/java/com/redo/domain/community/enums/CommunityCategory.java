package com.redo.domain.community.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommunityCategory {

    INFORMATION_SHARING("정보공유", 1),
    REWARD_REVIEW("리워드후기", 2),
    ENVIRONMENTAL_PRACTICE("환경실천", 3);


    private final String description;
    private final int code;
}
