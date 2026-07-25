package com.redo.domain.contribution.service;

import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.repository.CertificationRepository;
import com.redo.domain.contribution.converter.ContributionConverter;
import com.redo.domain.contribution.dto.res.MyContributionResponseDTO;
import com.redo.domain.contribution.exception.ContributionException;
import com.redo.domain.contribution.exception.code.ContributionErrorCode;
import com.redo.domain.user.entity.UserProfile;
import com.redo.domain.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContributionService {

    private final CertificationRepository certificationRepository;
    private final UserProfileRepository userProfileRepository;

    // 나의 기여도 조회 로직
    public MyContributionResponseDTO getMyContribution(Long userId) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ContributionException(
                        ContributionErrorCode.CONTRIBUTION_NOT_FOUND
                ));

        long totalCertificationCount = certificationRepository.countByUserIdAndStatus(
                userId,
                CertificationStatus.PASSED
        );

        return ContributionConverter.toMyContributionResponse(
                userProfile.getNickname(),
                totalCertificationCount
        );
    }
}
