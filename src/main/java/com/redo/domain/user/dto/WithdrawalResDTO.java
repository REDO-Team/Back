package com.redo.domain.user.dto;

import java.util.List;

public class WithdrawalResDTO {

    // 탈퇴 사유 목록
    public record ReasonList(
        List<ReasonInfo> reasons
    ){

    }
    // 탈퇴 사유 하나 정보
    public record ReasonInfo(
            Long reasonId,
            String label
    ){

    }
}