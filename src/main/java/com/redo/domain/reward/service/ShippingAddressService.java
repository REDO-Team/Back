package com.redo.domain.reward.service;

import com.redo.domain.reward.client.JusoAddressSearchClient;
import com.redo.domain.reward.converter.ShippingAddressConverter;
import com.redo.domain.reward.dto.req.ShippingAddressCreateRequestDTO;
import com.redo.domain.reward.dto.req.ShippingAddressUpdateRequestDTO;
import com.redo.domain.reward.dto.res.ShippingAddressDeleteResponseDTO;
import com.redo.domain.reward.dto.res.ShippingAddressListResponseDTO;
import com.redo.domain.reward.dto.res.ShippingAddressResponseDTO;
import com.redo.domain.reward.dto.res.ShippingAddressSearchResponseDTO;
import com.redo.domain.reward.entity.ShippingAddress;
import com.redo.domain.reward.exception.ShippingAddressException;
import com.redo.domain.reward.exception.code.ShippingAddressErrorCode;
import com.redo.domain.reward.repository.ShippingAddressRepository;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.exception.UserErrorCode;
import com.redo.domain.user.repository.UserRepository;
import com.redo.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingAddressService {

    private final ShippingAddressRepository shippingAddressRepository;
    private final UserRepository userRepository;
    private final JusoAddressSearchClient jusoAddressSearchClient;

    // 배송지 주소 검색 로직
    public ShippingAddressSearchResponseDTO searchShippingAddress(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            throw new ShippingAddressException(ShippingAddressErrorCode.INVALID_SHIPPING_ADDRESS_REQUEST);
        }

        if (page < 1 || size < 1 || size > 100) {
            throw new ShippingAddressException(ShippingAddressErrorCode.INVALID_PAGE_REQUEST);
        }

        return jusoAddressSearchClient.search(keyword.trim(), page, size);
    }

    // 배송지 목록 조회 로직
    public ShippingAddressListResponseDTO getShippingAddresses(Long userId) {
        User user = getUser(userId);

        return ShippingAddressConverter.toShippingAddressListResponse(
                shippingAddressRepository.findByUserAndDeletedAtIsNullOrderByIsDefaultDescUpdatedAtDescIdDesc(user)
        );
    }

    // 배송지 생성 로직
    @Transactional
    public ShippingAddressResponseDTO createShippingAddress(
            Long userId,
            ShippingAddressCreateRequestDTO request
    ) {
        User user = getUserForUpdate(userId);
        boolean hasActiveAddress = shippingAddressRepository.existsByUserAndDeletedAtIsNull(user);
        boolean shouldBeDefault = !hasActiveAddress || Boolean.TRUE.equals(request.isDefault());

        if (shouldBeDefault) {
            shippingAddressRepository.clearDefaultByUser(user);
        }

        ShippingAddress shippingAddress = ShippingAddressConverter.toShippingAddress(
                request,
                user,
                shouldBeDefault
        );

        return ShippingAddressConverter.toShippingAddressResponse(
                shippingAddressRepository.save(shippingAddress)
        );
    }

    // 배송지 수정 로직
    @Transactional
    public ShippingAddressResponseDTO updateShippingAddress(
            Long userId,
            Long shippingAddressId,
            ShippingAddressUpdateRequestDTO request
    ) {
        User user = getUserForUpdate(userId);
        ShippingAddress shippingAddress = getActiveShippingAddress(shippingAddressId, user);
        boolean shouldBeDefault = resolveDefaultStatus(shippingAddress, request);

        if (shouldBeDefault) {
            shippingAddressRepository.clearDefaultByUserExcept(user, shippingAddressId);
        }

        shippingAddress.update(
                request.addressType(),
                request.receiverName(),
                request.phone(),
                request.postalCode(),
                request.address1(),
                request.address2(),
                shouldBeDefault
        );

        return ShippingAddressConverter.toShippingAddressResponse(shippingAddress);
    }

    // 배송지 삭제 로직
    @Transactional
    public ShippingAddressDeleteResponseDTO deleteShippingAddress(Long userId, Long shippingAddressId) {
        User user = getUserForUpdate(userId);
        ShippingAddress shippingAddress = getActiveShippingAddress(shippingAddressId, user);
        boolean wasDefault = Boolean.TRUE.equals(shippingAddress.getIsDefault());

        shippingAddress.delete();

        if (wasDefault) {
            shippingAddressRepository.flush();
            shippingAddressRepository.findFirstByUserAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(user)
                    .ifPresent(ShippingAddress::setDefault);
        }

        return ShippingAddressConverter.toShippingAddressDeleteResponse(shippingAddressId);
    }

    private boolean resolveDefaultStatus(
            ShippingAddress shippingAddress,
            ShippingAddressUpdateRequestDTO request
    ) {
        if (Boolean.TRUE.equals(request.isDefault())) {
            return true;
        }

        return Boolean.TRUE.equals(shippingAddress.getIsDefault());
    }

    private ShippingAddress getActiveShippingAddress(Long shippingAddressId, User user) {
        return shippingAddressRepository.findByIdAndUserAndDeletedAtIsNull(shippingAddressId, user)
                .orElseThrow(() -> new ShippingAddressException(ShippingAddressErrorCode.SHIPPING_ADDRESS_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    private User getUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }
}
