package com.redo.domain.user.controller;

import com.redo.domain.user.dto.ProfileResDTO;
import com.redo.domain.user.exception.AuthErrorCode;
import com.redo.domain.user.exception.ProfileSuccessCode;
import com.redo.domain.user.service.ProfileService;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.apiPayload.exception.GeneralException;
import com.redo.global.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final JwtUtil jwtUtil;

    @GetMapping("/me")
    public ApiResponse<ProfileResDTO.ProfileInfo> getProfile(
            @RequestHeader("Authorization") String authHeader
    ) {
        String accessToken = authHeader.replace("Bearer ", "");
        Long userId;
        try{
            userId = jwtUtil.getUserIdFromToken(accessToken);
        }catch(Exception e){
            throw new GeneralException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }
        ProfileResDTO.ProfileInfo responseBody = profileService.getProfile(userId);

        return ApiResponse.onSuccess(ProfileSuccessCode.GET_PROFILE_SUCCESS, responseBody);
    }
}