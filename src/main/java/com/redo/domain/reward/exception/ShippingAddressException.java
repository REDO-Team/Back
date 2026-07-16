package com.redo.domain.reward.exception;

import com.redo.domain.reward.exception.code.ShippingAddressErrorCode;
import com.redo.global.apiPayload.exception.GeneralException;

public class ShippingAddressException extends GeneralException {

    public ShippingAddressException(ShippingAddressErrorCode code) {
        super(code);
    }
}
