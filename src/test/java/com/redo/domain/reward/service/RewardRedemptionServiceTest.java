package com.redo.domain.reward.service;

import com.redo.domain.point.entity.PointTransaction;
import com.redo.domain.point.enums.PointTransactionType;
import com.redo.domain.point.exception.PointException;
import com.redo.domain.point.exception.code.PointErrorCode;
import com.redo.domain.point.repository.PointTransactionRepository;
import com.redo.domain.reward.dto.req.RewardRedemptionCreateRequestDTO;
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
import com.redo.domain.reward.enums.ShippingAddressType;
import com.redo.domain.reward.exception.RewardException;
import com.redo.domain.reward.exception.code.RewardErrorCode;
import com.redo.domain.reward.repository.RewardFulfillmentRepository;
import com.redo.domain.reward.repository.RewardProductRepository;
import com.redo.domain.reward.repository.RewardRedemptionRepository;
import com.redo.domain.reward.repository.ShippingAddressRepository;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.repository.UserRepository;
import com.redo.global.s3.service.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardRedemptionServiceTest {

    private static final String IDEMPOTENCY_KEY = "redemption-request-1";
    private static final String IMAGE_KEY = "reward-products/product-1.png";
    private static final String IMAGE_URL = "https://presigned.example.com/product-1.png";

    @Mock
    private RewardProductRepository rewardProductRepository;

    @Mock
    private RewardRedemptionRepository rewardRedemptionRepository;

    @Mock
    private RewardFulfillmentRepository rewardFulfillmentRepository;

    @Mock
    private ShippingAddressRepository shippingAddressRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private RewardRedemptionService rewardRedemptionService;

    @Test
    void redeemDeliveryProductUsesSavedShippingAddressAndDeductsPointAndStock() {
        User user = createUser(5_000);
        RewardProduct product = createProduct(RewardProductType.PARTNER_BRAND, 1_000, 1);
        ShippingAddress shippingAddress = createShippingAddress(user);
        stubSuccessfulRedemption(user, product);
        when(shippingAddressRepository.findByIdAndDeletedAtIsNull(3L))
                .thenReturn(Optional.of(shippingAddress));

        RewardRedemptionResponseDTO result = rewardRedemptionService.redeem(
                1L,
                IDEMPOTENCY_KEY,
                new RewardRedemptionCreateRequestDTO(2L, 3L, null, null)
        );

        assertThat(result.rewardRedemptionId()).isEqualTo(4L);
        assertThat(result.usedPoint()).isEqualTo(1_000);
        assertThat(result.remainingPoint()).isEqualTo(4_000);
        assertThat(user.getTotalPoints()).isEqualTo(4_000);
        assertThat(product.getStockQuantity()).isZero();
        assertThat(product.getStatus()).isEqualTo(RewardProductStatus.SOLD_OUT);

        ArgumentCaptor<RewardRedemption> redemptionCaptor = ArgumentCaptor.forClass(RewardRedemption.class);
        verify(rewardRedemptionRepository).save(redemptionCaptor.capture());
        assertThat(redemptionCaptor.getValue().getProductName()).isEqualTo("리워드 상품");
        assertThat(redemptionCaptor.getValue().getProductImageKey()).isEqualTo(IMAGE_KEY);
        assertThat(redemptionCaptor.getValue().getPostalCode()).isEqualTo("12345");
        assertThat(redemptionCaptor.getValue().getAddress1()).isEqualTo("서울시 중구");

        ArgumentCaptor<RewardFulfillment> fulfillmentCaptor = ArgumentCaptor.forClass(RewardFulfillment.class);
        verify(rewardFulfillmentRepository).save(fulfillmentCaptor.capture());
        assertThat(fulfillmentCaptor.getValue().getRewardFulfillmentType())
                .isEqualTo(RewardFulfillmentType.DELIVERY);

        ArgumentCaptor<PointTransaction> transactionCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getTransactionType()).isEqualTo(PointTransactionType.USE);
        assertThat(transactionCaptor.getValue().getAmount()).isEqualTo(1_000);
    }

    @Test
    void redeemCouponUsesRequestRecipient() {
        User user = createUser(5_000);
        RewardProduct product = createProduct(RewardProductType.COUPON_GIFTICON, 5_000, 10);
        stubSuccessfulRedemption(user, product);

        rewardRedemptionService.redeem(
                1L,
                IDEMPOTENCY_KEY,
                new RewardRedemptionCreateRequestDTO(2L, null, "김지구", "010-1234-5678")
        );

        ArgumentCaptor<RewardRedemption> redemptionCaptor = ArgumentCaptor.forClass(RewardRedemption.class);
        verify(rewardRedemptionRepository).save(redemptionCaptor.capture());
        RewardRedemption redemption = redemptionCaptor.getValue();
        assertThat(redemption.getShippingAddress()).isNull();
        assertThat(redemption.getReceiverName()).isEqualTo("김지구");
        assertThat(redemption.getReceiverPhone()).isEqualTo("010-1234-5678");

        ArgumentCaptor<RewardFulfillment> fulfillmentCaptor = ArgumentCaptor.forClass(RewardFulfillment.class);
        verify(rewardFulfillmentRepository).save(fulfillmentCaptor.capture());
        assertThat(fulfillmentCaptor.getValue().getRewardFulfillmentType())
                .isEqualTo(RewardFulfillmentType.COUPON);
    }

    @Test
    void redeemThrowsWhenPointIsInsufficient() {
        User user = createUser(500);
        RewardProduct product = createProduct(RewardProductType.COUPON_GIFTICON, 1_000, 10);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(rewardRedemptionRepository.existsByUserAndIdempotencyKey(user, IDEMPOTENCY_KEY))
                .thenReturn(false);
        when(rewardProductRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> rewardRedemptionService.redeem(
                1L,
                IDEMPOTENCY_KEY,
                new RewardRedemptionCreateRequestDTO(2L, null, "김지구", "01012345678")
        )).isInstanceOfSatisfying(PointException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(PointErrorCode.INSUFFICIENT_POINT)
        );

        verify(rewardRedemptionRepository, never()).save(any());
        verify(rewardFulfillmentRepository, never()).save(any());
        verify(pointTransactionRepository, never()).save(any());
    }

    @Test
    void redeemRejectsDuplicateRequest() {
        User user = createUser(5_000);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(rewardRedemptionRepository.existsByUserAndIdempotencyKey(user, IDEMPOTENCY_KEY))
                .thenReturn(true);

        assertThatThrownBy(() -> rewardRedemptionService.redeem(
                1L,
                IDEMPOTENCY_KEY,
                new RewardRedemptionCreateRequestDTO(2L, null, "김지구", "01012345678")
        )).isInstanceOfSatisfying(RewardException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(RewardErrorCode.DUPLICATE_REDEMPTION_REQUEST)
        );

        verify(rewardProductRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void getMyRedemptionsReturnsPresignedProductImageUrl() {
        User user = createUser(4_000);
        RewardProduct product = createProduct(RewardProductType.PARTNER_BRAND, 1_000, 9);
        RewardRedemption redemption = RewardRedemption.builder()
                .id(4L)
                .user(user)
                .rewardProduct(product)
                .productName(product.getName())
                .productImageKey(product.getImageKey())
                .receiverName("김지구")
                .receiverPhone("01012345678")
                .usedPoint(1_000)
                .idempotencyKey(IDEMPOTENCY_KEY)
                .build();
        ReflectionTestUtils.setField(redemption, "createdAt", LocalDateTime.of(2026, 7, 21, 12, 0));
        RewardFulfillment fulfillment = RewardFulfillment.builder()
                .id(5L)
                .rewardRedemption(redemption)
                .rewardFulfillmentType(RewardFulfillmentType.DELIVERY)
                .status(RewardFulfillmentStatus.READY)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rewardFulfillmentRepository.findPageByUserWithRedemptionAndProduct(user, pageable))
                .thenReturn(new PageImpl<>(List.of(fulfillment), pageable, 1));
        when(s3Service.createPresignedUrl(IMAGE_KEY)).thenReturn(IMAGE_URL);

        Page<RewardRedemptionHistoryResponseDTO> result =
                rewardRedemptionService.getMyRedemptions(1L, pageable);

        assertThat(result.getContent()).singleElement().satisfies(history -> {
            assertThat(history.rewardRedemptionId()).isEqualTo(4L);
            assertThat(history.productImageUrl()).isEqualTo(IMAGE_URL);
            assertThat(history.fulfillmentType()).isEqualTo(RewardFulfillmentType.DELIVERY);
            assertThat(history.fulfillmentStatus()).isEqualTo(RewardFulfillmentStatus.READY);
        });
        verify(s3Service).createPresignedUrl(IMAGE_KEY);
    }

    private void stubSuccessfulRedemption(User user, RewardProduct product) {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(rewardRedemptionRepository.existsByUserAndIdempotencyKey(user, IDEMPOTENCY_KEY))
                .thenReturn(false);
        when(rewardProductRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(product));
        when(rewardRedemptionRepository.save(any(RewardRedemption.class))).thenAnswer(invocation -> {
            RewardRedemption redemption = invocation.getArgument(0);
            ReflectionTestUtils.setField(redemption, "id", 4L);
            return redemption;
        });
    }

    private User createUser(int point) {
        User user = User.createGeneral("testuser", "test@example.com", "encoded-password");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.addPoint(point);
        return user;
    }

    private RewardProduct createProduct(RewardProductType type, int pricePoint, int stockQuantity) {
        return RewardProduct.builder()
                .id(2L)
                .rewardProductType(type)
                .name("리워드 상품")
                .description("상품 설명")
                .imageKey(IMAGE_KEY)
                .pricePoint(pricePoint)
                .stockQuantity(stockQuantity)
                .status(RewardProductStatus.ACTIVE)
                .build();
    }

    private ShippingAddress createShippingAddress(User user) {
        return ShippingAddress.builder()
                .id(3L)
                .user(user)
                .addressType(ShippingAddressType.HOME)
                .receiverName("김지구")
                .phone("01012345678")
                .postalCode("12345")
                .address1("서울시 중구")
                .address2("101호")
                .isDefault(true)
                .build();
    }
}
