package org.marsik.bugautomation.stats;

public enum SingleStat {
    TRIGGER_COUNT("trigger_count", StatType.COUNTER),
    TRIGGER_TIME("trigger_run_time", StatType.GAUGE),
    SPRINT_CONTENT("sprint_content", StatType.GAUGE);

    private final String name;
    private final StatType type;

    SingleStat(String name, StatType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public StatType getType() {
        return type;
    }
}
