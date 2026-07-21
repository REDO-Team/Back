package com.redo.domain.reward.facade;

import com.redo.domain.reward.dto.req.RewardRedemptionCreateRequestDTO;
import com.redo.domain.reward.dto.res.RewardRedemptionResponseDTO;
import com.redo.domain.reward.exception.RewardException;
import com.redo.domain.reward.exception.code.RewardErrorCode;
import com.redo.domain.reward.service.RewardRedemptionService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RewardRedemptionFacade {

    private static final String PRODUCT_LOCK_KEY_PREFIX = "lock:reward-redemption:product:";
    private static final long LOCK_WAIT_TIME_SECONDS = 3L;

    private final RedissonClient redissonClient;
    private final RewardRedemptionService rewardRedemptionService;

    // 상품별 분산 락을 획득한 뒤 리워드 상품 구매 트랜잭션을 실행하는 로직
    public RewardRedemptionResponseDTO redeem(
            Long userId,
            String idempotencyKey,
            RewardRedemptionCreateRequestDTO request
    ) {
        RLock lock = redissonClient.getLock(createProductLockKey(request.rewardProductId()));
        boolean acquired = false;

        try {
            acquired = lock.tryLock(LOCK_WAIT_TIME_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RewardException(RewardErrorCode.REDEMPTION_LOCK_ACQUISITION_FAILED);
            }

            return rewardRedemptionService.redeem(userId, idempotencyKey, request);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RewardException(RewardErrorCode.REDEMPTION_LOCK_ACQUISITION_FAILED);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String createProductLockKey(Long rewardProductId) {
        return PRODUCT_LOCK_KEY_PREFIX + rewardProductId;
    }
}
