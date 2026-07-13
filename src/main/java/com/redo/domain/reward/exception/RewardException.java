package com.redo.domain.reward.exception;

import com.redo.domain.reward.exception.code.RewardErrorCode;
import com.redo.global.apiPayload.exception.GeneralException;

public class RewardException extends GeneralException {
    public RewardException(RewardErrorCode code) {
        super(code);
    }
}
