package com.redo.domain.user.dto;

import com.redo.domain.user.enums.SignupType;
import jakarta.validation.constraints.*;

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

            @Pattern(
                    regexp = "^[a-zA-Z0-9]{6,}$",
                    message = "올바른 아이디 형식이 아닙니다."
            )
            String loginId,

            @Email
            String email,

            @Pattern(
                    regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9!@#$%^&*()_+\\-=]{8,16}$",
                    message = "8~16자의 영문, 숫자를 조합해 주세요."
            )
            String password,

            String socialProvider,     // 원래 enum값이지만 일반가입의 경우 null이므로 string으로 받고 필요시 valueOf.
            String socialId,

            @NotNull
            List<Long> agreedTermsIds
    ) {
        @AssertTrue(message = "일반 가입 시 loginId, email, password는 필수입니다.")
        public boolean isGeneralValid() {
            if (signupType != SignupType.GENERAL) return true;
            return loginId != null && email != null && password != null;
        }
        @AssertTrue(message = "소셜 가입 시 socialProvider, socialId는 필수입니다.")
        public boolean isSocialValid() {
            if (signupType != SignupType.SOCIAL) return true;
            return socialProvider != null && socialId != null;
        }
    }

}