package com.redo.domain.contribution.enums;

public enum ContributionMilestone {

    TOILET_PAPER("화장지", 3, "를"),
    NOTE("노트", 5, "를"),
    GLASS_BOTTLE("유리병", 7, "을"),
    TRASH_BAG("쓰레기 봉투", 10, "를"),
    PLASTIC_FLOWER_POT("플라스틱 화분", 15, "을"),
    T_SHIRT("티셔츠", 30, "를"),
    SNEAKERS("운동화", 50, "를"),
    BENCH("벤치", 100, "를");

    private final String displayName;
    private final int requiredCount;
    private final String objectParticle;

    ContributionMilestone(String displayName, int requiredCount, String objectParticle) {
        this.displayName = displayName;
        this.requiredCount = requiredCount;
        this.objectParticle = objectParticle;
    }

    public String displayName() {
        return displayName;
    }

    public int requiredCount() {
        return requiredCount;
    }

    public String objectParticle() {
        return objectParticle;
    }
}
