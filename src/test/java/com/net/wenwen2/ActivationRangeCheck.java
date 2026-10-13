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
        checkPlayerIndex();
        checkEntityState();
    }
    private static void checkEntityState() {
        Object policy = new Object(), dimension = new Object();
        EntityDistanceState state = new EntityDistanceState();
        state.update(policy, dimension, 100, 105, 1, 2, 3, true);
        check(state.matches(policy, dimension, 1, 2, 3), "cache same entity position");
        check(!state.matches(new Object(), dimension, 1, 2, 3), "config/tag invalidates cache");
        check(!state.matches(policy, new Object(), 1, 2, 3), "dimension invalidates cache");
        check(!state.matches(policy, dimension, 2, 2, 3), "movement invalidates cache");
        check(state.canSkip(104), "cached suspension before deadline");
        check(!state.canSkip(105), "cached suspension deadline");
        check(!state.canSkip(99), "cached suspension clock rollback");
        state.update(policy, dimension, 105, 110, 1, 2, 3, false);
        check(!state.canSkip(106), "active entity never skips distance checks");
        for (int interval : new int[] {1, 5, 10, 200}) {
            for (long now : new long[] {-201, -1, 0, 100, 10001}) {
                int[] phases = new int[interval];
                for (int id = 0; id < interval; id++) {
                    long deadline = FrozenCheckSchedule.nextCheck(now, id, interval);
                    check(deadline > now && deadline <= now + interval, "staggered wait bound");
                    phases[(int) (deadline - now - 1)]++;
                    check(Math.floorMod(deadline, interval) == Math.floorMod(id, interval), "stable entity phase");
                }
                for (int count : phases) check(count == 1, "no synchronized check burst");
            }
        }
        check(FrozenCheckSchedule.advanceIdleTime(0) == 1, "frozen idle timer advances");
        check(FrozenCheckSchedule.advanceIdleTime(Integer.MAX_VALUE) == Integer.MAX_VALUE, "idle timer never wraps");
        System.out.println("Passed mutable cache invalidation, recovery, staggered scheduling and idle timer checks.");
    }
    private static void checkPlayerIndex() {
        java.util.Random random = new java.util.Random(20261013L);
        PlayerRangeIndex index = new PlayerRangeIndex();
        int checks = 0;
        for (boolean cylinder : new boolean[] {false, true}) {
            for (double radius : new double[] {1, 64, 32768}) {
                for (int count : new int[] {0, 1, 8, 32, 128, 129, 512, 129, 1, 0}) {
                    double[][] players = new double[count][3];
                    index.reset(radius * radius, radius / 2, cylinder);
                    for (int i = 0; i < count; i++) {
                        for (int axis = 0; axis < 3; axis++)
                            players[i][axis] = (random.nextDouble() - 0.5) * radius * 20;
                        index.add(players[i][0], players[i][1], players[i][2]);
                    }
                    index.build();
                    check(index.hasPlayers() == (count > 0), "player presence");
                    for (int q = 0; q < 1000; q++) {
                        double x = (random.nextDouble() - 0.5) * radius * 20;
                        double y = (random.nextDouble() - 0.5) * radius * 20;
                        double z = (random.nextDouble() - 0.5) * radius * 20;
                        boolean expected = false;
                        for (double[] p : players) expected |= ActivationRange.contains(
                            x - p[0], y - p[1], z - p[2], cylinder, radius * radius, radius / 2);
                        check(index.contains(x, y, z) == expected, "indexed/brute force agreement");
                        checks++;
                    }
                }
                // Exercise the bucket path with exact boundaries and negative cell coordinates.
                index.reset(radius * radius, radius / 2, cylinder);
                for (int i = 0; i < 129; i++) index.add(-radius, 0, -radius);
                index.build();
                check(index.contains(0, 0, -radius), "indexed radius boundary");
                check(!index.contains(radius * 0.00001, 0, -radius), "indexed outside radius");
                check(index.contains(-radius, -radius / 2, -radius), "indexed vertical boundary");
            }
        }
        System.out.println("Passed " + checks + " indexed/brute force comparisons and bucket boundary checks.");
    }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
