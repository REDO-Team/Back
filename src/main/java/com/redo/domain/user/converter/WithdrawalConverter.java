package com.redo.domain.user.converter;

import com.redo.domain.user.dto.WithdrawalResDTO;
import com.redo.domain.user.entity.UserWithdrawalReason;

import java.util.List;

public class WithdrawalConverter {

    // UserWithdrawalReason 하나 → ReasonInfo 하나로 변환
    public static WithdrawalResDTO.ReasonInfo toReasonInfo(UserWithdrawalReason reason) {
        return new WithdrawalResDTO.ReasonInfo(reason.getId(), reason.getLabel());
    }

    // List<UserWithdrawalReason> → ReasonList로 변환
    public static WithdrawalResDTO.ReasonList toReasonList(List<UserWithdrawalReason> reasons) {
        List<WithdrawalResDTO.ReasonInfo> reasonInfoList = reasons.
                stream().map(WithdrawalConverter::toReasonInfo).toList();

        return new WithdrawalResDTO.ReasonList(reasonInfoList);
    }
}