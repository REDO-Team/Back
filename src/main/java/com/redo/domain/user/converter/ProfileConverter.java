package com.redo.domain.user.converter;

import com.redo.domain.user.dto.ProfileResDTO;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.entity.UserProfile;

public class ProfileConverter {

    public static ProfileResDTO.ProfileInfo toProfileInfo(User user, UserProfile profile) {
        return new ProfileResDTO.ProfileInfo(user.getId(), profile.getNickname(), profile.getProfileImageUrl(), user.getTotalPoints());
    }

}