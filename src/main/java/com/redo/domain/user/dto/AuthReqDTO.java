package com.redo.domain.user.dto;

import com.redo.domain.user.enums.SignupType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

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

    //회원가입
    public record Signup(
            @NotNull
            SignupType signupType,

            String loginId,
            String email,
            String password,

            String socialProvider,     // 원래 enum값이지만 일반가입의 경우 null이므로 string으로 받고 필요시 valueOf.
            String socialId,

            @NotNull
            List<Long> agreedTermsIds
    ) {}

}