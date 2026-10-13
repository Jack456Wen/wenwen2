package com.net.wenwen2;

import java.util.Arrays;

/** Reusable player positions and horizontal buckets; exact geometry decides activation. */
final class PlayerRangeIndex {
    // Nine bucket probes cost more than a short contiguous scan on small servers.
    private static final int LINEAR_LIMIT = 128;
    private double[] xs = new double[8], ys = new double[8], zs = new double[8];
    private int[] next = new int[8], heads = new int[16];
    private long[] keys = new long[16];
    private int size;
    private double radius, radiusSquared, vertical;
    private boolean cylinder;

    void reset(double radiusSquared, double vertical, boolean cylinder) {
        size = 0;
        this.radiusSquared = radiusSquared;
        this.radius = Math.sqrt(radiusSquared);
        this.vertical = vertical;
        this.cylinder = cylinder;
    }

    void add(double x, double y, double z) {
        if (size == xs.length) {
            int capacity = size * 2;
            xs = Arrays.copyOf(xs, capacity);
            ys = Arrays.copyOf(ys, capacity);
            zs = Arrays.copyOf(zs, capacity);
            next = Arrays.copyOf(next, capacity);
        }
        xs[size] = x;
        ys[size] = y;
        zs[size++] = z;
    }

    void build() {
        if (size <= LINEAR_LIMIT) return;
        int capacity = 16;
        while (capacity < size * 2) capacity *= 2;
        if (heads.length < capacity) {
            heads = new int[capacity];
            keys = new long[capacity];
        }
        Arrays.fill(heads, -1);
        for (int i = 0; i < size; i++) {
            long key = key(cell(xs[i]), cell(zs[i]));
            int slot = slot(key);
            keys[slot] = key;
            next[i] = heads[slot];
            heads[slot] = i;
        }
    }

    boolean hasPlayers() { return size != 0; }

    boolean contains(double x, double y, double z) {
        if (size <= LINEAR_LIMIT) {
            for (int i = 0; i < size; i++) if (matches(i, x, y, z)) return true;
            return false;
        }
        int cx = cell(x), cz = cell(z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int slot = slot(key(cx + dx, cz + dz));
                for (int i = heads[slot]; i != -1; i = next[i]) {
                    if (matches(i, x, y, z)) return true;
                }
            }
        }
        return false;
    }

    private boolean matches(int i, double x, double y, double z) {
        return ActivationRange.contains(x - xs[i], y - ys[i], z - zs[i],
            cylinder, radiusSquared, vertical);
    }

    private int cell(double coordinate) { return (int) Math.floor(coordinate / radius); }
    private static long key(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }
    private int slot(long key) {
        long hash = key;
        hash = (hash ^ (hash >>> 33)) * 0xff51afd7ed558ccdL;
        hash = (hash ^ (hash >>> 33)) * 0xc4ceb9fe1a85ec53L;
        int slot = (int) (hash ^ (hash >>> 33)) & (heads.length - 1);
        while (heads[slot] != -1 && keys[slot] != key) slot = (slot + 1) & (heads.length - 1);
        return slot;
    }
}
