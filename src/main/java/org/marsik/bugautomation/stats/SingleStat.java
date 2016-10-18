package org.marsik.bugautomation.stats;

public enum SingleStat {
    TRIGGER_COUNT("bug_automation_trigger_count", StatType.COUNTER),
    TRIGGER_TIME("bug_automation_trigger_run_time", StatType.GAUGE),
    SPRINT_CONTENT("bug_automation_sprint_content", StatType.GAUGE);

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
