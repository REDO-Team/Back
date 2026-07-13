package com.redo.domain.user.dto;

import jakarta.validation.constraints.Email;
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

    // 이메일 인증 요청
    public record EmailVerifyRequest(
            @NotBlank
            @Email
            String email
    ) {}

    // 이메일 인증번호 확인
    public record EmailVerifyConfirm(
            @NotBlank
            @Email
            String email,
            @NotBlank
            String code
    ) {}

}