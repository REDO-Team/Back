package com.redo.domain.contribution.converter;

import com.redo.domain.contribution.dto.res.MyContributionResponseDTO;
import com.redo.domain.contribution.enums.ContributionMilestone;
import com.redo.domain.contribution.enums.ContributionMilestoneStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ContributionConverterTest {

    private static final String NICKNAME = "지구";

    @ParameterizedTest
    @MethodSource("milestoneBoundaryCases")
    void convertsMilestoneBoundaries(
            long certificationCount,
            ContributionMilestone expectedLatest,
            ContributionMilestone expectedNext,
            long expectedRemainingCount
    ) {
        MyContributionResponseDTO response =
                ContributionConverter.toMyContributionResponse(NICKNAME, certificationCount);

        assertThat(response.remainingCount()).isEqualTo(expectedRemainingCount);
        assertMilestoneType(response.latestAchievedMilestone(), expectedLatest);
        assertMilestoneType(response.nextMilestone(), expectedNext);
        assertMilestoneStatuses(response, certificationCount, expectedNext);
        assertSummaryMessage(response, certificationCount, expectedLatest, expectedRemainingCount);
    }

    private static Stream<Arguments> milestoneBoundaryCases() {
        return Stream.of(
                Arguments.of(0L, null, ContributionMilestone.TOILET_PAPER, 3L),
                Arguments.of(2L, null, ContributionMilestone.TOILET_PAPER, 1L),
                Arguments.of(3L, ContributionMilestone.TOILET_PAPER, ContributionMilestone.NOTE, 2L),
                Arguments.of(4L, ContributionMilestone.TOILET_PAPER, ContributionMilestone.NOTE, 1L),
                Arguments.of(5L, ContributionMilestone.NOTE, ContributionMilestone.GLASS_BOTTLE, 2L),
                Arguments.of(6L, ContributionMilestone.NOTE, ContributionMilestone.GLASS_BOTTLE, 1L),
                Arguments.of(7L, ContributionMilestone.GLASS_BOTTLE, ContributionMilestone.TRASH_BAG, 3L),
                Arguments.of(8L, ContributionMilestone.GLASS_BOTTLE, ContributionMilestone.TRASH_BAG, 2L),
                Arguments.of(9L, ContributionMilestone.GLASS_BOTTLE, ContributionMilestone.TRASH_BAG, 1L),
                Arguments.of(10L, ContributionMilestone.TRASH_BAG, ContributionMilestone.PLASTIC_FLOWER_POT, 5L),
                Arguments.of(11L, ContributionMilestone.TRASH_BAG, ContributionMilestone.PLASTIC_FLOWER_POT, 4L),
                Arguments.of(14L, ContributionMilestone.TRASH_BAG, ContributionMilestone.PLASTIC_FLOWER_POT, 1L),
                Arguments.of(15L, ContributionMilestone.PLASTIC_FLOWER_POT, ContributionMilestone.T_SHIRT, 15L),
                Arguments.of(16L, ContributionMilestone.PLASTIC_FLOWER_POT, ContributionMilestone.T_SHIRT, 14L),
                Arguments.of(29L, ContributionMilestone.PLASTIC_FLOWER_POT, ContributionMilestone.T_SHIRT, 1L),
                Arguments.of(30L, ContributionMilestone.T_SHIRT, ContributionMilestone.SNEAKERS, 20L),
                Arguments.of(31L, ContributionMilestone.T_SHIRT, ContributionMilestone.SNEAKERS, 19L),
                Arguments.of(49L, ContributionMilestone.T_SHIRT, ContributionMilestone.SNEAKERS, 1L),
                Arguments.of(50L, ContributionMilestone.SNEAKERS, ContributionMilestone.BENCH, 50L),
                Arguments.of(51L, ContributionMilestone.SNEAKERS, ContributionMilestone.BENCH, 49L),
                Arguments.of(99L, ContributionMilestone.SNEAKERS, ContributionMilestone.BENCH, 1L),
                Arguments.of(100L, ContributionMilestone.BENCH, null, 0L),
                Arguments.of(101L, ContributionMilestone.BENCH, null, 0L)
        );
    }

    private void assertMilestoneType(
            MyContributionResponseDTO.MilestoneDTO milestone,
            ContributionMilestone expectedType
    ) {
        if (expectedType == null) {
            assertThat(milestone).isNull();
            return;
        }

        assertThat(milestone.type()).isEqualTo(expectedType);
    }

    private void assertMilestoneStatuses(
            MyContributionResponseDTO response,
            long certificationCount,
            ContributionMilestone expectedNext
    ) {
        assertThat(response.milestones()).allSatisfy(milestone -> {
            ContributionMilestoneStatus expectedStatus;
            if (certificationCount >= milestone.requiredCertificationCount()) {
                expectedStatus = ContributionMilestoneStatus.ACHIEVED;
            } else if (milestone.type() == expectedNext) {
                expectedStatus = ContributionMilestoneStatus.IN_PROGRESS;
            } else {
                expectedStatus = ContributionMilestoneStatus.LOCKED;
            }

            assertThat(milestone.status()).isEqualTo(expectedStatus);
        });
    }

    private void assertSummaryMessage(
            MyContributionResponseDTO response,
            long certificationCount,
            ContributionMilestone expectedLatest,
            long expectedRemainingCount
    ) {
        if (expectedLatest == null) {
            assertThat(response.summaryMessage()).isEqualTo(
                    "%s님! 첫 번째 재활용 물품까지 %d회 남았어요!".formatted(
                            NICKNAME,
                            expectedRemainingCount
                    )
            );
            return;
        }

        assertThat(response.summaryMessage()).isEqualTo(
                "%s님! 지금까지 %d번의 분리수거로 %s%s 만들었어요!".formatted(
                        NICKNAME,
                        certificationCount,
                        expectedLatest.displayName(),
                        expectedLatest.objectParticle()
                )
        );
    }
}
