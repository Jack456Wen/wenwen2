package com.net.wenwen2;

public final class FrozenCheckSchedule {
    // World time continues advancing while entity.tickCount is frozen.
    public static boolean canSkip(long now, long checkedAt, int interval) {
        return now >= checkedAt && now - checkedAt < interval;
    }
    private FrozenCheckSchedule() {}
}
