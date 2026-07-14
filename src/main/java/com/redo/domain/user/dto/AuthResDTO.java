package com.redo.domain.user.dto;

public class AuthResDTO {

    // 일반 로그인 응답
    public record Login(
            Long userId,
            String accessToken
    ) {
    }

    // 소셜 로그인 응답 (기존/신규 공통)
    public record SocialLogin(
            Long userId,
            Boolean isNewUser,
            String accessToken,
            String socialProvider,
            String socialId
    ) {
    }

    // 토큰 재발급 응답
    public record Reissue(
            String accessToken
    ) {
    }

    //로그인 중복 확인시 응답
    public record LoginIdCheck(
            Boolean isAvailable
    ){
    }

    public record Signup(
        Long userId,
        String accessToken
    ) {

    }
}
