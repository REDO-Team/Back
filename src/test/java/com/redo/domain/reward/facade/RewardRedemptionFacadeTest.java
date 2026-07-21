package com.redo.domain.reward.facade;

import com.redo.domain.reward.dto.req.RewardRedemptionCreateRequestDTO;
import com.redo.domain.reward.dto.res.RewardRedemptionResponseDTO;
import com.redo.domain.reward.exception.RewardException;
import com.redo.domain.reward.exception.code.RewardErrorCode;
import com.redo.domain.reward.service.RewardRedemptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardRedemptionFacadeTest {

    private static final String IDEMPOTENCY_KEY = "redemption-request-1";
    private static final String LOCK_KEY = "lock:reward-redemption:product:2";

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private RewardRedemptionService rewardRedemptionService;

    @InjectMocks
    private RewardRedemptionFacade rewardRedemptionFacade;

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void redeemExecutesTransactionAfterAcquiringProductLock() throws InterruptedException {
        RewardRedemptionCreateRequestDTO request = createRequest();
        RewardRedemptionResponseDTO expected = new RewardRedemptionResponseDTO(
                4L,
                2L,
                "리워드 상품",
                1_000,
                4_000
        );
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(3L, TimeUnit.SECONDS)).thenReturn(true);
        when(rewardRedemptionService.redeem(1L, IDEMPOTENCY_KEY, request)).thenReturn(expected);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        RewardRedemptionResponseDTO result = rewardRedemptionFacade.redeem(
                1L,
                IDEMPOTENCY_KEY,
                request
        );

        assertThat(result).isEqualTo(expected);
        InOrder inOrder = inOrder(lock, rewardRedemptionService);
        inOrder.verify(lock).tryLock(3L, TimeUnit.SECONDS);
        inOrder.verify(rewardRedemptionService).redeem(1L, IDEMPOTENCY_KEY, request);
        inOrder.verify(lock).isHeldByCurrentThread();
        inOrder.verify(lock).unlock();
    }

    @Test
    void redeemThrowsWhenProductLockCannotBeAcquired() throws InterruptedException {
        RewardRedemptionCreateRequestDTO request = createRequest();
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(3L, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> rewardRedemptionFacade.redeem(1L, IDEMPOTENCY_KEY, request))
                .isInstanceOfSatisfying(RewardException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(RewardErrorCode.REDEMPTION_LOCK_ACQUISITION_FAILED)
                );

        verify(rewardRedemptionService, never()).redeem(1L, IDEMPOTENCY_KEY, request);
        verify(lock, never()).unlock();
    }

    @Test
    void redeemReleasesLockWhenTransactionFails() throws InterruptedException {
        RewardRedemptionCreateRequestDTO request = createRequest();
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(3L, TimeUnit.SECONDS)).thenReturn(true);
        when(rewardRedemptionService.redeem(1L, IDEMPOTENCY_KEY, request))
                .thenThrow(new IllegalStateException("구매 처리 실패"));
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        assertThatThrownBy(() -> rewardRedemptionFacade.redeem(1L, IDEMPOTENCY_KEY, request))
                .isInstanceOf(IllegalStateException.class);

        verify(lock).unlock();
    }

    @Test
    void redeemPreservesInterruptStatusWhenLockWaitIsInterrupted() throws InterruptedException {
        RewardRedemptionCreateRequestDTO request = createRequest();
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(3L, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

        assertThatThrownBy(() -> rewardRedemptionFacade.redeem(1L, IDEMPOTENCY_KEY, request))
                .isInstanceOfSatisfying(RewardException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(RewardErrorCode.REDEMPTION_LOCK_ACQUISITION_FAILED)
                );

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        verify(rewardRedemptionService, never()).redeem(1L, IDEMPOTENCY_KEY, request);
        verify(lock, never()).unlock();
    }

    @Test
    void redeemDoesNotUnlockWhenCurrentThreadNoLongerOwnsLock() throws InterruptedException {
        RewardRedemptionCreateRequestDTO request = createRequest();
        RewardRedemptionResponseDTO expected = new RewardRedemptionResponseDTO(
                4L,
                2L,
                "리워드 상품",
                1_000,
                4_000
        );
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(3L, TimeUnit.SECONDS)).thenReturn(true);
        when(rewardRedemptionService.redeem(1L, IDEMPOTENCY_KEY, request)).thenReturn(expected);
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        RewardRedemptionResponseDTO result = rewardRedemptionFacade.redeem(
                1L,
                IDEMPOTENCY_KEY,
                request
        );

        assertThat(result).isEqualTo(expected);
        verify(lock, never()).unlock();
    }

    private RewardRedemptionCreateRequestDTO createRequest() {
        return new RewardRedemptionCreateRequestDTO(2L, 3L, null, null);
    }
}
