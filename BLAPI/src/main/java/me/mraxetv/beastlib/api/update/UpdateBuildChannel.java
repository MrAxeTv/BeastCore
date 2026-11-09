package me.mraxetv.beastlib.api.update;

public enum UpdateBuildChannel {
    FREE,
    PREMIUM,
    INVALID,
    UNKNOWN;

    public boolean isPremium() {
        return this == PREMIUM;
    }

    public boolean isFree() {
        return this == FREE;
    }

    public boolean isInvalid() {
        return this == INVALID;
    }
}
