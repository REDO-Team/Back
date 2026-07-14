package com.redo.domain.user.service;

import com.redo.domain.user.converter.ProfileConverter;
import com.redo.domain.user.dto.ProfileResDTO;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.entity.UserProfile;
import com.redo.domain.user.exception.ProfileErrorCode;
import com.redo.domain.user.repository.UserProfileRepository;
import com.redo.domain.user.repository.UserRepository;
import com.redo.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public ProfileResDTO.ProfileInfo getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ProfileErrorCode.USER_NOT_FOUND));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new GeneralException(ProfileErrorCode.USER_NOT_FOUND));

        return ProfileConverter.toProfileInfo(user, profile);
    }
}