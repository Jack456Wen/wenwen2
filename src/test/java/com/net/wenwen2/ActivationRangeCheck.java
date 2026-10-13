package com.net.wenwen2;

public final class ActivationRangeCheck {
    public static void main(String[] args) {
        check(ActivationRange.contains(64, 0, 0, false, 4096, 32), "sphere boundary");
        check(!ActivationRange.contains(64.001, 0, 0, false, 4096, 32), "outside sphere");
        check(!ActivationRange.contains(64, 32, 0, false, 4096, 32), "sphere diagonal");
        check(ActivationRange.contains(64, 32, 0, true, 4096, 32), "cylinder corner boundary");
        check(ActivationRange.contains(-64, -32, 0, true, 4096, 32), "negative coordinates");
        check(!ActivationRange.contains(0, 32.001, 0, true, 4096, 32), "vertical limit");
        check(!ActivationRange.contains(64.001, 0, 0, true, 4096, 32), "horizontal limit");
        check(!ActivationRange.contains(48, 0, 48, true, 4096, 32), "circular horizontal radius");
        System.out.println("Passed 8 activation range checks.");
        check(FrozenCheckSchedule.canSkip(109, 100, 10), "frozen before deadline");
        check(!FrozenCheckSchedule.canSkip(110, 100, 10), "frozen deadline recheck");
        check(!FrozenCheckSchedule.canSkip(150, 100, 10), "time jump recheck");
        check(!FrozenCheckSchedule.canSkip(90, 100, 10), "time rollback recheck");
        check(!FrozenCheckSchedule.canSkip(101, 100, 1), "interval one checks every tick");
        System.out.println("Passed 5 frozen check scheduling checks.");
    }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
