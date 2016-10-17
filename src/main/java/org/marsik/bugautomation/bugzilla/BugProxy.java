package org.marsik.bugautomation.bugzilla;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.marsik.bugautomation.facts.BugzillaBugFlag;

public class BugProxy {
    private final Map<String, Object> map;
    private final Set<BugzillaBugFlag> flags = new HashSet<>();

    public BugProxy(Map<String, Object> map) {
        this.map = map;
    }

    public String getId() {
        return (String)map.get("id").toString();
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
        return Arrays.asList(getAs("cf_verified", String[].class));
    }

    public String get(String key) {
        return (String)map.get(key);
    }

    @SuppressWarnings("unchecked")
    private <T> T getAs(String key, Class<T> cls) {
        return (T)map.get(key);
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
            final Object o = ((Object[]) value)[0];
            if (o.toString().equals("---")) return null;
            else return o;
        } else {
            return value;
        }
    }

    public void loadFlags(BugProxy data) {
        for (Object flag0: data.getAs("flags", Object[].class)) {
            Map<String,Object> flag = (Map<String,Object>)flag0;
            flags.add(new BugzillaBugFlag(flag));
        }
    }

    public Set<BugzillaBugFlag> getFlags() {
        return Collections.unmodifiableSet(flags);
    }

    public List<String> getKeywords() {
        return Arrays.asList(getAs("keywords", String[].class));
    }

    public List<String> getBlocks() {
        return Arrays.asList(getAs("blocks", Integer[].class)).stream()
                .map(String::valueOf).collect(Collectors.toList());
    }
}
