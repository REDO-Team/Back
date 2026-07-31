package com.redo.domain.certification.enums;

public enum CertificationJudgementMode {
    CREATE_GENERAL(true, false),
    CREATE_AFTER_SEARCH(false, false),
    RETRY(false, true);

    private final boolean classificationRequired;
    private final boolean retry;

    CertificationJudgementMode(boolean classificationRequired, boolean retry) {
        this.classificationRequired = classificationRequired;
        this.retry = retry;
    }

    public static CertificationJudgementMode forCreate(CertificationSource source) {
        return source == CertificationSource.GENERAL
                ? CREATE_GENERAL
                : CREATE_AFTER_SEARCH;
    }

    public boolean classificationRequired() {
        return classificationRequired;
    }

    public boolean retry() {
        return retry;
    }
}
