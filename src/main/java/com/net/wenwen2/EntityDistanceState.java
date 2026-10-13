package com.net.wenwen2;

/** Mutable, entity-owned cache. Each logical side owns its own entity instance. */
public final class EntityDistanceState {
    Object policy, dimension;
    long checkedAt, nextCheck;
    double x, y, z, viewerX, viewerY, viewerZ;
    int viewerId;
    boolean suspended;

    boolean matches(Object policy, Object dimension, double x, double y, double z) {
        return this.policy == policy && this.dimension == dimension
            && this.x == x && this.y == y && this.z == z;
    }

    boolean canSkip(long now) { return suspended && now >= checkedAt && now < nextCheck; }

    void update(Object policy, Object dimension, long now, long nextCheck,
                double x, double y, double z, boolean suspended) {
        this.policy = policy;
        this.dimension = dimension;
        this.checkedAt = now;
        this.nextCheck = nextCheck;
        this.x = x;
        this.y = y;
        this.z = z;
        this.suspended = suspended;
    }
}
