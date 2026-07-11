package com.redo.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthReqDTO {

    // 일반 로그인 요청
    public record Login(
            @NotBlank
            String loginId,

            @NotBlank
            String password
    ) {}

    // 소셜 로그인 요청
    public record SocialLogin(
            @NotBlank
            String accessToken
    ) {}

}