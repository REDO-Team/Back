package com.redo.domain.reward.controller;

import com.redo.domain.reward.dto.req.ShippingAddressCreateRequestDTO;
import com.redo.domain.reward.dto.req.ShippingAddressUpdateRequestDTO;
import com.redo.domain.reward.dto.res.ShippingAddressDeleteResponseDTO;
import com.redo.domain.reward.dto.res.ShippingAddressListResponseDTO;
import com.redo.domain.reward.dto.res.ShippingAddressResponseDTO;
import com.redo.domain.reward.dto.res.ShippingAddressSearchResponseDTO;
import com.redo.domain.reward.exception.code.ShippingAddressSuccessCode;
import com.redo.domain.reward.service.ShippingAddressService;
import com.redo.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shipping-addresses")
public class ShippingAddressController {

    private final ShippingAddressService shippingAddressService;

    // 배송지 주소 검색 API
    @GetMapping("/search")
    public ApiResponse<ShippingAddressSearchResponseDTO> searchShippingAddress(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(
                ShippingAddressSuccessCode.SEARCH_SHIPPING_ADDRESS_SUCCESS,
                shippingAddressService.searchShippingAddress(keyword, page, size)
        );
    }

    // 배송지 목록 조회 API
    @GetMapping
    public ApiResponse<ShippingAddressListResponseDTO> getShippingAddresses(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(
                ShippingAddressSuccessCode.GET_SHIPPING_ADDRESSES_SUCCESS,
                shippingAddressService.getShippingAddresses(userId)
        );
    }

    // 배송지 생성 API
    @PostMapping
    public ApiResponse<ShippingAddressResponseDTO> createShippingAddress(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ShippingAddressCreateRequestDTO request
    ) {
        return ApiResponse.onSuccess(
                ShippingAddressSuccessCode.CREATE_SHIPPING_ADDRESS_SUCCESS,
                shippingAddressService.createShippingAddress(userId, request)
        );
    }

    // 배송지 수정 API
    @PatchMapping("/{shippingAddressId}")
    public ApiResponse<ShippingAddressResponseDTO> updateShippingAddress(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long shippingAddressId,
            @Valid @RequestBody ShippingAddressUpdateRequestDTO request
    ) {
        return ApiResponse.onSuccess(
                ShippingAddressSuccessCode.UPDATE_SHIPPING_ADDRESS_SUCCESS,
                shippingAddressService.updateShippingAddress(userId, shippingAddressId, request)
        );
    }

    // 배송지 삭제 API
    @DeleteMapping("/{shippingAddressId}")
    public ApiResponse<ShippingAddressDeleteResponseDTO> deleteShippingAddress(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long shippingAddressId
    ) {
        return ApiResponse.onSuccess(
                ShippingAddressSuccessCode.DELETE_SHIPPING_ADDRESS_SUCCESS,
                shippingAddressService.deleteShippingAddress(userId, shippingAddressId)
        );
    }
}
