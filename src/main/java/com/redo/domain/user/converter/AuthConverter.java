package com.redo.domain.user.converter;

import com.redo.domain.user.dto.AuthResDTO;
import com.redo.domain.user.entity.User;

public class AuthConverter {

    public static AuthResDTO.Login toLoginResponse(User user, String accessToken) {
        return new AuthResDTO.Login(user.getId(), accessToken);
    }
    public static AuthResDTO.SocialLogin toSocialLoginResponse(
            User user,              // 기존회원이면 값 있음, 신규면 null
            Boolean isNewUser,
            String accessToken,     // 기존회원이면 값 있음, 신규면 null
            String socialProvider,  // 신규회원용 (User가 없을 때 대비)
            String socialId         // 신규회원용 (User가 없을 때 대비)
    ) {
        return new AuthResDTO.SocialLogin(
                user != null ? user.getId() : null,
                isNewUser,
                accessToken,
                socialProvider,
                socialId
        );
    }
    public static AuthResDTO.Reissue toReissueResponse(String accessToken){
        return new AuthResDTO.Reissue(accessToken);
    }



}