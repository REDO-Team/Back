package com.redo.domain.user.dto;

public class ProfileResDTO {

    public record ProfileInfo(
            Long userId,
            String nickname,
            String profileImageUrl,
            Integer totalPoint
    ) {}

}