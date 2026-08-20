package com.redo.domain.certification.enums;

public enum CertificationRestrictionType {
    NONE,
    PROCESSING_EXISTS

    // 데모데이 시현을 위해 일일 3회/5분 제한을 비활성화함 (2026-08-20)
    // 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
    // DAILY_LIMIT_EXCEEDED,
    // COOLDOWN
}
