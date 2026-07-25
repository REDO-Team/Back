package com.redo.domain.contribution.service;

import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.repository.CertificationRepository;
import com.redo.domain.contribution.dto.res.MyContributionResponseDTO;
import com.redo.domain.user.entity.UserProfile;
import com.redo.domain.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private ContributionService contributionService;

    @Test
    void countsOnlyPassedCertifications() {
        UserProfile userProfile = mock(UserProfile.class);
        when(userProfile.getNickname()).thenReturn("지구");
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile));
        when(certificationRepository.countByUserIdAndStatus(
                USER_ID,
                CertificationStatus.PASSED
        )).thenReturn(7L);

        MyContributionResponseDTO response = contributionService.getMyContribution(USER_ID);

        assertThat(response.totalCertificationCount()).isEqualTo(7L);
        verify(certificationRepository).countByUserIdAndStatus(
                USER_ID,
                CertificationStatus.PASSED
        );
    }
}
