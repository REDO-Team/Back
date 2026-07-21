package com.redo.domain.user.service;

import com.redo.domain.user.exception.AuthErrorCode;
import com.redo.global.apiPayload.exception.GeneralException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SocialOAuthService {

    // 구글 사용자 정보 조회
    public SocialUserInfo getGoogleUserInfo(String accessToken) {
        WebClient webClient = WebClient.create("https://www.googleapis.com");

        GoogleResponse response = webClient.get()
                .uri("/oauth2/v2/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(GoogleResponse.class)
                .block();

        if (response == null || response.email() == null) {
            throw new GeneralException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
        return new SocialUserInfo(response.id(), response.email());
    }

    // 여러 소셜 서비스의 결과를 통일해서 담는 공통 타입
    public record SocialUserInfo(
            String socialId,
            String email
    ) {
    }

    // 구글이 실제로 응답하는 JSON 형태
    private record GoogleResponse(
            String id,
            String email
    ) {
    }

    public SocialUserInfo getKakaoUserInfo(String accessToken) {
        WebClient webClient = WebClient.create("https://kapi.kakao.com");

        KakaoResponse response = webClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoResponse.class)
                .block();
        if (response == null || response.kakao_account() == null || response.kakao_account().email() == null) {
            throw new GeneralException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        return new SocialUserInfo(
                String.valueOf(response.id()),      // Long → String 변환
                response.kakao_account().email()    // 중첩 구조에서 두 단계로 꺼냄
        );
    }

    private record KakaoResponse(
            Long id,                    // 바깥 상자의 id
            KakaoAccount kakao_account  // 바깥 상자 안에, "KakaoAccount"라는 또 다른 상자
    ) {
        private record KakaoAccount(
                String email             // 안쪽 상자 안의 email
        ) {
        }

    }

    // 네이버 사용자 정보 조회
    public SocialUserInfo getNaverUserInfo(String accessToken) {
        WebClient webClient = WebClient.create("https://openapi.naver.com");

        NaverResponse response = webClient.get()
                .uri("/v1/nid/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(NaverResponse.class)
                .block();

        if (response == null || response.response() == null || response.response().email() == null) {
            throw new GeneralException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        return new SocialUserInfo(
                response.response().id(),
                response.response().email()
        );
    }

    private record NaverResponse(
            NaverAccount response  // 네이버 "response"라는 상자로 한 번 더 감싸져 있음
    ) {
        private record NaverAccount(
                String id,
                String email
        ) {
        }
    }
}