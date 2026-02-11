package com.jxsh.misc;

import java.util.HashMap;
import java.util.Map;

/**
 * Robust cache for network-wide player counts.
 * Updated via Plugin Message from Velocity.
 */
public class NetworkCache {
    private int globalTotalCount = 0;
    private final Map<String, Integer> serverCounts = new HashMap<>();

    public void setGlobalTotalCount(int count) {
        this.globalTotalCount = count;
    }

    public int getGlobalTotalCount() {
        return globalTotalCount;
    }

    public void setServerCount(String server, int count) {
        serverCounts.put(server, count);
    }

    public int getServerCount(String server) {
        return serverCounts.getOrDefault(server, 0);
    }

    public void clearServerCounts() {
        serverCounts.clear();
    }
}
