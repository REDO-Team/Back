package com.redo.domain.user.dto;

import jakarta.validation.constraints.NotNull;

public class WithdrawalReqDTO {
    // 탈퇴사유 id 요청
    public record Withdrawal(
            @NotNull
            Long reasonId
    ){
    }
}
