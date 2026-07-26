package com.redo.domain.reward.service;

import com.redo.domain.point.entity.PointTransaction;
import com.redo.domain.point.enums.PointTransactionType;
import com.redo.domain.point.exception.PointException;
import com.redo.domain.point.exception.code.PointErrorCode;
import com.redo.domain.point.repository.PointTransactionRepository;
import com.redo.domain.reward.converter.RewardRedemptionConverter;
import com.redo.domain.reward.dto.req.RewardRedemptionCreateRequestDTO;
import com.redo.domain.reward.dto.res.RewardRedemptionHistoryPageResponseDTO;
import com.redo.domain.reward.dto.res.RewardRedemptionHistoryResponseDTO;
import com.redo.domain.reward.dto.res.RewardRedemptionResponseDTO;
import com.redo.domain.reward.entity.RewardFulfillment;
import com.redo.domain.reward.entity.RewardProduct;
import com.redo.domain.reward.entity.RewardRedemption;
import com.redo.domain.reward.entity.ShippingAddress;
import com.redo.domain.reward.enums.RewardFulfillmentStatus;
import com.redo.domain.reward.enums.RewardFulfillmentType;
import com.redo.domain.reward.enums.RewardProductStatus;
import com.redo.domain.reward.enums.RewardProductType;
import com.redo.domain.reward.exception.RewardException;
import com.redo.domain.reward.exception.ShippingAddressException;
import com.redo.domain.reward.exception.code.RewardErrorCode;
import com.redo.domain.reward.exception.code.ShippingAddressErrorCode;
import com.redo.domain.reward.repository.RewardFulfillmentRepository;
import com.redo.domain.reward.repository.RewardProductRepository;
import com.redo.domain.reward.repository.RewardRedemptionRepository;
import com.redo.domain.reward.repository.ShippingAddressRepository;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.exception.UserErrorCode;
import com.redo.domain.user.repository.UserRepository;
import com.redo.global.apiPayload.exception.GeneralException;
import com.redo.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardRedemptionService {

    private static final int REDEMPTION_QUANTITY = 1;
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final RewardProductRepository rewardProductRepository;
    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final RewardFulfillmentRepository rewardFulfillmentRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    // 리워드 상품 구매 로직
    @Transactional
    public RewardRedemptionResponseDTO redeem(
            Long userId,
            String idempotencyKey,
            RewardRedemptionCreateRequestDTO request
    ) {
        String normalizedIdempotencyKey = validateAndNormalizeIdempotencyKey(idempotencyKey);
        User user = getUserForUpdate(userId);

        if (rewardRedemptionRepository.existsByUserAndIdempotencyKey(user, normalizedIdempotencyKey)) {
            throw new RewardException(RewardErrorCode.DUPLICATE_REDEMPTION_REQUEST);
        }

        RewardProduct rewardProduct = getRewardProductForUpdate(request.rewardProductId());
        validateRewardProduct(rewardProduct);

        Recipient recipient = resolveRecipient(user, rewardProduct.getRewardProductType(), request);
        validatePoint(user, rewardProduct.getPricePoint());

        RewardRedemption redemption = RewardRedemption.builder()
                .user(user)
                .rewardProduct(rewardProduct)
                .shippingAddress(recipient.shippingAddress())
                .productName(rewardProduct.getName())
                .productImageKey(rewardProduct.getImageKey())
                .receiverName(recipient.receiverName())
                .receiverPhone(recipient.receiverPhone())
                .postalCode(recipient.postalCode())
                .address1(recipient.address1())
                .address2(recipient.address2())
                .usedPoint(rewardProduct.getPricePoint())
                .idempotencyKey(normalizedIdempotencyKey)
                .build();

        RewardRedemption savedRedemption = rewardRedemptionRepository.save(redemption);

        user.usePoint(rewardProduct.getPricePoint());
        rewardProduct.decreaseStock();

        rewardFulfillmentRepository.save(RewardFulfillment.builder()
                .rewardRedemption(savedRedemption)
                .rewardFulfillmentType(resolveFulfillmentType(rewardProduct.getRewardProductType()))
                .status(RewardFulfillmentStatus.READY)
                .build());

        pointTransactionRepository.save(PointTransaction.builder()
                .user(user)
                .rewardRedemption(savedRedemption)
                .transactionType(PointTransactionType.USE)
                .amount(rewardProduct.getPricePoint())
                .idempotencyKey("reward-redemption:" + savedRedemption.getId())
                .build());

        return RewardRedemptionConverter.toRewardRedemptionResponse(
                savedRedemption,
                user.getTotalPoints()
        );
    }

    // 로그인한 사용자의 리워드 상품 구매 내역 조회 로직
    public RewardRedemptionHistoryPageResponseDTO getMyRedemptions(
            Long userId,
            Long cursor,
            int size
    ) {
        User user = getUser(userId);
        List<RewardFulfillment> fulfillments =
                rewardFulfillmentRepository.findByUserWithRedemptionAndProduct(
                        user,
                        cursor,
                        PageRequest.of(0, size + 1)
                );
        boolean hasNext = fulfillments.size() > size;
        List<RewardFulfillment> pageFulfillments = hasNext
                ? fulfillments.subList(0, size)
                : fulfillments;
        List<RewardRedemptionHistoryResponseDTO> content = pageFulfillments.stream()
                .map(fulfillment -> RewardRedemptionConverter.toRewardRedemptionHistoryResponse(
                        fulfillment,
                        createProductImageUrl(
                                fulfillment.getRewardRedemption().getProductImageKey()
                        )
                ))
                .toList();
        Long nextCursor = hasNext && !pageFulfillments.isEmpty()
                ? pageFulfillments.get(pageFulfillments.size() - 1)
                        .getRewardRedemption()
                        .getId()
                : null;

        return RewardRedemptionConverter.toRewardRedemptionHistoryPageResponse(
                content,
                nextCursor,
                hasNext
        );
    }

    private Recipient resolveRecipient(
            User user,
            RewardProductType rewardProductType,
            RewardRedemptionCreateRequestDTO request
    ) {
        if (rewardProductType == RewardProductType.PARTNER_BRAND) {
            ShippingAddress shippingAddress = getShippingAddress(user, request.shippingAddressId());
            return new Recipient(
                    shippingAddress,
                    shippingAddress.getReceiverName(),
                    shippingAddress.getPhone(),
                    shippingAddress.getPostalCode(),
                    shippingAddress.getAddress1(),
                    shippingAddress.getAddress2()
            );
        }

        if (request.receiverName() == null || request.receiverName().isBlank()
                || request.receiverPhone() == null || request.receiverPhone().isBlank()) {
            throw new RewardException(RewardErrorCode.COUPON_RECIPIENT_REQUIRED);
        }

        return new Recipient(
                null,
                request.receiverName().trim(),
                request.receiverPhone().trim(),
                null,
                null,
                null
        );
    }

    private ShippingAddress getShippingAddress(User user, Long shippingAddressId) {
        if (shippingAddressId == null) {
            throw new RewardException(RewardErrorCode.SHIPPING_ADDRESS_REQUIRED);
        }

        ShippingAddress shippingAddress = shippingAddressRepository
                .findByIdAndDeletedAtIsNull(shippingAddressId)
                .orElseThrow(() -> new ShippingAddressException(
                        ShippingAddressErrorCode.SHIPPING_ADDRESS_NOT_FOUND
                ));

        if (!shippingAddress.getUser().getId().equals(user.getId())) {
            throw new ShippingAddressException(ShippingAddressErrorCode.SHIPPING_ADDRESS_FORBIDDEN);
        }

        return shippingAddress;
    }

    private void validateRewardProduct(RewardProduct rewardProduct) {
        if (rewardProduct.getStockQuantity() < REDEMPTION_QUANTITY
                || rewardProduct.getStatus() == RewardProductStatus.SOLD_OUT) {
            throw new RewardException(RewardErrorCode.REWARD_PRODUCT_OUT_OF_STOCK);
        }

        if (rewardProduct.getStatus() != RewardProductStatus.ACTIVE) {
            throw new RewardException(RewardErrorCode.REWARD_PRODUCT_NOT_ACTIVE);
        }
    }

    private void validatePoint(User user, Integer pricePoint) {
        if (user.getTotalPoints() < pricePoint) {
            throw new PointException(PointErrorCode.INSUFFICIENT_POINT);
        }
    }

    private String validateAndNormalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new RewardException(RewardErrorCode.INVALID_IDEMPOTENCY_KEY);
        }

        return idempotencyKey.trim();
    }

    private RewardFulfillmentType resolveFulfillmentType(RewardProductType rewardProductType) {
        return switch (rewardProductType) {
            case PARTNER_BRAND -> RewardFulfillmentType.DELIVERY;
            case COUPON_GIFTICON -> RewardFulfillmentType.COUPON;
        };
    }

    private RewardProduct getRewardProductForUpdate(Long rewardProductId) {
        return rewardProductRepository.findByIdForUpdate(rewardProductId)
                .orElseThrow(() -> new RewardException(RewardErrorCode.REWARD_PRODUCT_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    private User getUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    // S3 객체 키를 구매 내역의 상품 이미지 Presigned URL로 변환하는 로직
    private String createProductImageUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }

        return s3Service.createPresignedUrl(imageKey);
    }

    private record Recipient(
            ShippingAddress shippingAddress,
            String receiverName,
            String receiverPhone,
            String postalCode,
            String address1,
            String address2
    ) {
    }
}
