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
import com.redo.global.s3.service.S3Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


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

        String imageUrl;
        if (profile.getProfileImageKey() != null) {
            imageUrl = s3Service.createPresignedUrl(profile.getProfileImageKey());
        } else {
            imageUrl = profile.getCharacterCode();
        }

        return ProfileConverter.toProfileInfo(user, profile, imageUrl);
    }


    @Transactional
    public void updateNickname(Long userId, String nickname) {
        if(userProfileRepository.findByNickname(nickname).isPresent()){
            throw new GeneralException(ProfileErrorCode.DUPLICATE_NICKNAME);
        }
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(ProfileErrorCode.USER_NOT_FOUND));

        try {
            profile.updateNickname(nickname);
            userProfileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(ProfileErrorCode.DUPLICATE_NICKNAME);
        }
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

        try {
            userProfileRepository.saveAndFlush(userProfile);
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(ProfileErrorCode.DUPLICATE_NICKNAME);
        }
        return new ProfileResDTO.CreateProfile(user.getId());
    }


    @Transactional
    public ProfileResDTO.ProfileImage updateProfileImage(Long userId, MultipartFile file) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(ProfileErrorCode.USER_NOT_FOUND));

        String oldImageKey = profile.getProfileImageKey();
        String newImageKey = s3Service.upload(file, "profiles");

        try {
            profile.updateProfileImageKey(newImageKey);
            userProfileRepository.saveAndFlush(profile);

            String presignedUrl = s3Service.createPresignedUrl(newImageKey);

            if (oldImageKey != null) {
                s3Service.delete(oldImageKey);
            }

            return new ProfileResDTO.ProfileImage(presignedUrl);
        } catch (Exception e) {
            s3Service.delete(newImageKey);
            throw e;
        }
    }

}