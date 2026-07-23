package com.redo.domain.certification.enums;

public enum CertificationSource {
    GENERAL(50),
    AFTER_SEARCH(100);

    private final int rewardPoint;

    CertificationSource(int rewardPoint) {
        this.rewardPoint = rewardPoint;
    }

    public int rewardPoint() {
        return rewardPoint;
    }
}
