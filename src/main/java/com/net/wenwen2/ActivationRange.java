package com.net.wenwen2;

/** Pure geometry, shared with boundary verification. */
public final class ActivationRange {
    public static boolean contains(double dx, double dy, double dz, boolean cylinder,
                                   double radiusSquared, double verticalDistance) {
        if (cylinder) return Math.abs(dy) <= verticalDistance && dx * dx + dz * dz <= radiusSquared;
        return dx * dx + dy * dy + dz * dz <= radiusSquared;
    }
    private ActivationRange() {}
}
