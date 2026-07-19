package com.redo.domain.reward.service;

import com.redo.domain.reward.dto.res.RewardProductDetailResponseDTO;
import com.redo.domain.reward.dto.res.RewardProductResponseDTO;
import com.redo.domain.reward.entity.RewardProduct;
import com.redo.domain.reward.enums.RewardProductStatus;
import com.redo.domain.reward.enums.RewardProductType;
import com.redo.domain.reward.exception.RewardException;
import com.redo.domain.reward.exception.code.RewardErrorCode;
import com.redo.domain.reward.repository.RewardProductRepository;
import com.redo.global.s3.service.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardProductServiceTest {

    private static final String IMAGE_KEY = "reward-products/product-1.png";
    private static final String IMAGE_URL = "https://presigned.example.com/product-1.png";

    @Mock
    private RewardProductRepository rewardProductRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private RewardProductService rewardProductService;

    @Test
    void getRewardProductsCreatesPresignedImageUrl() {
        Pageable pageable = PageRequest.of(0, 10);
        RewardProduct rewardProduct = createRewardProduct(RewardProductType.PARTNER_BRAND);
        when(rewardProductRepository.findByStatusAndStockQuantityGreaterThan(
                RewardProductStatus.ACTIVE,
                0,
                pageable
        )).thenReturn(new PageImpl<>(List.of(rewardProduct), pageable, 1));
        when(s3Service.createPresignedUrl(IMAGE_KEY)).thenReturn(IMAGE_URL);

        Page<RewardProductResponseDTO> result = rewardProductService.getRewardProducts(null, pageable);

        assertThat(result.getContent()).singleElement().satisfies(response -> {
            assertThat(response.rewardProductId()).isEqualTo(1L);
            assertThat(response.imageUrl()).isEqualTo(IMAGE_URL);
        });
        verify(s3Service).createPresignedUrl(IMAGE_KEY);
    }

    @Test
    void getRewardProductsFiltersByProductTypeAndCreatesPresignedImageUrl() {
        Pageable pageable = PageRequest.of(0, 10);
        RewardProduct rewardProduct = createRewardProduct(RewardProductType.COUPON_GIFTICON);
        when(rewardProductRepository.findByRewardProductTypeAndStatusAndStockQuantityGreaterThan(
                RewardProductType.COUPON_GIFTICON,
                RewardProductStatus.ACTIVE,
                0,
                pageable
        )).thenReturn(new PageImpl<>(List.of(rewardProduct), pageable, 1));
        when(s3Service.createPresignedUrl(IMAGE_KEY)).thenReturn(IMAGE_URL);

        Page<RewardProductResponseDTO> result = rewardProductService.getRewardProducts(
                RewardProductType.COUPON_GIFTICON,
                pageable
        );

        assertThat(result.getContent()).singleElement().satisfies(response -> {
            assertThat(response.rewardProductType()).isEqualTo(RewardProductType.COUPON_GIFTICON);
            assertThat(response.imageUrl()).isEqualTo(IMAGE_URL);
        });
        verify(s3Service).createPresignedUrl(IMAGE_KEY);
    }

    @Test
    void getRewardProductCreatesPresignedImageUrl() {
        RewardProduct rewardProduct = createRewardProduct(RewardProductType.PARTNER_BRAND);
        when(rewardProductRepository.findByIdAndStatusAndStockQuantityGreaterThan(
                1L,
                RewardProductStatus.ACTIVE,
                0
        )).thenReturn(Optional.of(rewardProduct));
        when(s3Service.createPresignedUrl(IMAGE_KEY)).thenReturn(IMAGE_URL);

        RewardProductDetailResponseDTO result = rewardProductService.getRewardProduct(1L);

        assertThat(result.rewardProductId()).isEqualTo(1L);
        assertThat(result.imageUrl()).isEqualTo(IMAGE_URL);
        verify(s3Service).createPresignedUrl(IMAGE_KEY);
    }

    @Test
    void getRewardProductThrowsWhenProductIsUnavailable() {
        when(rewardProductRepository.findByIdAndStatusAndStockQuantityGreaterThan(
                1L,
                RewardProductStatus.ACTIVE,
                0
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rewardProductService.getRewardProduct(1L))
                .isInstanceOfSatisfying(RewardException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(RewardErrorCode.REWARD_PRODUCT_NOT_FOUND)
                );
    }

    private RewardProduct createRewardProduct(RewardProductType rewardProductType) {
        return RewardProduct.builder()
                .id(1L)
                .rewardProductType(rewardProductType)
                .name("리워드 상품")
                .description("리워드 상품 설명")
                .usageGuide("사용 안내")
                .validityDays(30)
                .imageKey(IMAGE_KEY)
                .pricePoint(1_000)
                .stockQuantity(10)
                .status(RewardProductStatus.ACTIVE)
                .build();
    }
}
