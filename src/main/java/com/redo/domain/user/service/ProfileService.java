package com.redo.domain.user.service;

import com.redo.domain.user.converter.ProfileConverter;
import com.redo.domain.user.dto.ProfileReqDTO;
import com.redo.domain.user.dto.ProfileResDTO;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.entity.UserProfile;
import com.redo.domain.user.exception.ProfileErrorCode;
import com.redo.domain.user.repository.UserProfileRepository;
import com.redo.domain.user.repository.UserRepository;
import com.redo.global.apiPayload.exception.GeneralException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final S3Service s3Service;



    public ProfileResDTO.ProfileInfo getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ProfileErrorCode.USER_NOT_FOUND));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(ProfileErrorCode.USER_NOT_FOUND));

        return ProfileConverter.toProfileInfo(user, profile);
    }


    @Transactional
    public void updateNickname(Long userId, String nickname) {
        if(userProfileRepository.findByNickname(nickname).isPresent()){
            throw new GeneralException(ProfileErrorCode.DUPLICATE_NICKNAME);
        }
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(ProfileErrorCode.USER_NOT_FOUND));

        profile.updateNickname(nickname);
    }

    @Transactional
    public ProfileResDTO.CreateProfile createProfile(Long userId, ProfileReqDTO.CreateProfile request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ProfileErrorCode.USER_NOT_FOUND));

        if(userProfileRepository.findByUserId(userId).isPresent()){
            throw new GeneralException(ProfileErrorCode.PROFILE_ALREADY_EXISTS);
        }

        if(userProfileRepository.findByNickname(request.nickname()).isPresent()){
            throw new GeneralException(ProfileErrorCode.DUPLICATE_NICKNAME);
        }
        UserProfile userProfile = UserProfile.create(user, request.nickname(), request.characterCode(),
                request.gender(), request.birthDate());

        userProfileRepository.save(userProfile);

        return new ProfileResDTO.CreateProfile(user.getId());
    }

    @Transactional
    public ProfileResDTO.ProfileImage updateProfileImage(Long userId, MultipartFile file) throws IOException {

        // 파일 형식 체크
        String contentType = file.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new GeneralException(ProfileErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
        //파일 크기 검증
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new GeneralException(ProfileErrorCode.IMAGE_SIZE_EXCEEDED);
        }
        // 유저 프로필 조회
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(ProfileErrorCode.USER_NOT_FOUND));

        // S3에 파일 업로드
        String imageUrl = s3Service.uploadFile(file, userId);

        // 유저 프로필 업데이트
        profile.updateProfileImage(imageUrl);

        return new ProfileResDTO.ProfileImage(imageUrl);

    }

}