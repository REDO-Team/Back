package com.redo.domain.recycleGuide.service;

import com.redo.domain.recycleGuide.converter.RecycleGuideFavoriteConverter;
import com.redo.domain.recycleGuide.dto.RecycleGuideFavoriteResponseDTO;
import com.redo.domain.recycleGuide.entity.Mapping.RecycleGuideFavorite;
import com.redo.domain.recycleGuide.entity.RecycleGuide;
import com.redo.domain.recycleGuide.exception.RecycleGuideErrorCode;
import com.redo.domain.recycleGuide.repository.RecycleGuideFavoriteRepository;
import com.redo.domain.recycleGuide.repository.RecycleGuideRepository;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.repository.UserRepository;
import com.redo.global.apiPayload.code.GeneralErrorCode;
import com.redo.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecycleGuideFavoriteService {

    private final RecycleGuideFavoriteRepository recycleGuideFavoriteRepository;
    private final RecycleGuideRepository recycleGuideRepository;
    private final UserRepository userRepository;

    @Transactional
    public RecycleGuideFavoriteResponseDTO.FavoriteResultDTO addFavorite(Long userId, Long guideId) {


        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."));


        RecycleGuide guide = recycleGuideRepository.findById(guideId)
                .orElseThrow(() -> new GeneralException(RecycleGuideErrorCode.GUIDE_NOT_FOUND));


        if (recycleGuideFavoriteRepository.existsByUserIdAndRecycleGuideId(userId, guideId)) {
            throw new GeneralException(RecycleGuideErrorCode.FAVORITE_ALREADY_EXISTS);
        }


        RecycleGuideFavorite favorite = RecycleGuideFavorite.builder()
                .user(user)
                .recycleGuide(guide)
                .build();


        try {

            RecycleGuideFavorite savedFavorite = recycleGuideFavoriteRepository.save(favorite);
            return RecycleGuideFavoriteConverter.toFavoriteResultDTO(savedFavorite);

        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(RecycleGuideErrorCode.FAVORITE_ALREADY_EXISTS);
        }
    }
}