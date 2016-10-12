package org.marsik.bugautomation.stats;

public enum StatType {
    GAUGE(false),
    COUNTER(true),
    SUMMARY(false);

    private final boolean persistent;

    StatType(boolean persistent) {
        this.persistent = persistent;
    }

    public boolean isPersistent() {
        return persistent;
    }
}
