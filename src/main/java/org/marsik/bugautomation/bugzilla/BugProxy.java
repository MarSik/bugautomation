package org.marsik.bugautomation.bugzilla;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.marsik.bugautomation.facts.BugzillaBugFlag;

public class BugProxy {
    private final Map<String, Object> map;
    private final Set<BugzillaBugFlag> flags = new HashSet<>();

    public BugProxy(Map<String, Object> map) {
        this.map = map;
    }

    public String getCommunity() {
        return (String) map.getOrDefault("classification", "");
    }

    public String getId() {
        return (String)map.get("id").toString();
    }

    public Date getDate(String key) {
        return (Date)this.getAs(key, Date.class);
    }

    public Date getLastChangeTime() {
        return this.getDate("last_change_time");
    }

    public String getSummary() {
        return (String)map.get("summary");
    }

    public String getDescription() {
        return (String)map.get("description");
    }

    public String getStatus() {
        return (String)map.get("status");
    }

    public String getSeverity() {
        return (String)map.get("severity");
    }

    public String getPriority() {
        return (String)map.get("priority");
    }

    public List<String> getVerified() {
        return getList("cf_verified");
    }

    public String get(String key) {
        return (String)map.get(key);
    }

    @SuppressWarnings("unchecked")
    private <T> T getAs(String key, Class<T> cls) {
        return (T)map.get(key);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getList(String key) {
        T[] val = (T[])map.get(key);
        return Optional.ofNullable(val).map(Arrays::asList).orElse(Collections.emptyList());
    }

    public String getAssignedTo() {
        return (String)map.get("assigned_to");
    }

    public String getTargetRelease() {
        return (String) firstValue(map.get("target_release"));
    }


    public String getTargetMilestone() {
        return (String) firstValue(map.get("target_milestone"));
    }

    private Object firstValue(Object value) {
        if (value instanceof Object[]) {
            value = ((Object[]) value)[0];
        }

        if (value.toString().trim().equals("---")) return "";
        else return value;
    }

    @SuppressWarnings("unchecked")
    public void loadFlags(BugProxy data) {
        for (Object flag0: data.getList("flags")) {
            Map<String,Object> flag = (Map<String,Object>)flag0;
            flags.add(new BugzillaBugFlag(flag));
        }
    }

    public Set<BugzillaBugFlag> getFlags() {
        return Collections.unmodifiableSet(flags);
    }

    public List<String> getKeywords() {
        return getList("keywords");
    }

    public List<String> getBlocks() {
        return getList("blocks").stream()
                .map(String::valueOf).collect(Collectors.toList());
    }

    public String getPmScore() {
        return (String)map.get("cf_pm_score");
    }

    public String getWhiteBoard() {
        return (String)map.get("whiteboard");
    }
}
