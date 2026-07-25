package com.redo.domain.user.controller;

import com.redo.domain.user.dto.ProfileReqDTO;
import com.redo.domain.user.dto.ProfileResDTO;
import com.redo.domain.user.exception.AuthErrorCode;
import com.redo.domain.user.exception.ProfileSuccessCode;
import com.redo.domain.user.service.ProfileService;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.apiPayload.exception.GeneralException;
import com.redo.global.security.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "프로필", description = "사용자 프로필 조회 및 관리 API")
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

    @PatchMapping("/me/profile/nickname")
    public ApiResponse<Void> updateNickname(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid ProfileReqDTO.UpdateNickname request
    ) {
        String accessToken = authHeader.replace("Bearer ", "");
        Long userId;
        try{
            userId = jwtUtil.getUserIdFromToken(accessToken);
        }catch(Exception e){
            throw new GeneralException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }
        profileService.updateNickname(userId, request.nickname());

        return ApiResponse.onSuccess(ProfileSuccessCode.UPDATE_NICKNAME_SUCCESS, null);
    }

    @PostMapping("/me/profile")
    public ApiResponse<ProfileResDTO.CreateProfile> createProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid ProfileReqDTO.CreateProfile request
    ) {
        String accessToken = authHeader.replace("Bearer ", "");
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(accessToken);
        } catch (Exception e) {
            throw new GeneralException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        ProfileResDTO.CreateProfile responseBody = profileService.createProfile(userId, request);

        return ApiResponse.onSuccess(ProfileSuccessCode.CREATE_PROFILE_SUCCESS, responseBody);
    }

    @PatchMapping(value = "/me/profile/image", consumes = "multipart/form-data")
    public ApiResponse<ProfileResDTO.ProfileImage> updateProfileImage(
            @RequestHeader("Authorization") String authHeader,
            @RequestPart("profileImage") MultipartFile file
    ) {
        String accessToken = authHeader.replace("Bearer ", "");
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(accessToken);
        } catch (Exception e) {
            throw new GeneralException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        ProfileResDTO.ProfileImage responseBody = profileService.updateProfileImage(userId, file);

        return ApiResponse.onSuccess(ProfileSuccessCode.UPDATE_IMAGE_SUCCESS, responseBody);
    }

}
