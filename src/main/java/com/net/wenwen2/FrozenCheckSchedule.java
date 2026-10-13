package com.net.wenwen2;

public final class FrozenCheckSchedule {
    // World time continues advancing while entity.tickCount is frozen.
    public static boolean canSkip(long now, long checkedAt, int interval) {
        return now >= checkedAt && now - checkedAt < interval;
    }
    /** The first wait is staggered, subsequent checks stay on the entity's phase. */
    public static long nextCheck(long now, int entityId, int interval) {
        int phase = Math.floorMod(entityId, interval);
        int offset = Math.floorMod(phase - (int) Math.floorMod(now, (long) interval), interval);
        return now + (offset == 0 ? interval : offset);
    }
    public static int advanceIdleTime(int current) {
        return current == Integer.MAX_VALUE ? current : current + 1;
    }
    private FrozenCheckSchedule() {}
}
