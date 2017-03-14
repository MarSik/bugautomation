package org.marsik.bugautomation.stats;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Stats {
    Map<SingleStatWLabels, Double> values = new HashMap<>();

    public class ValueBuilder {
        private SingleStat base;
        private Map<String, String> labels = new HashMap<>();

        ValueBuilder(SingleStat base) {
            this.base = base;
        }

        public ValueBuilder label(String name, String value) {
            labels.put(name, value);
            return this;
        }

        public void value(int value) {
            value((double) value);
        }

        public void value(float value) {
            value((double) value);
        }

        public void value(Double value) {
            SingleStatWLabels statKey = SingleStatWLabels.builder()
                    .stat(base)
                    .build();

            labels.forEach((k, v) -> statKey.getLabels().add(new LabelValue(k, v)));
            values.putIfAbsent(statKey, 0d);
            Double v = values.get(statKey) + value;
            values.put(statKey, v);
        }
    }

    public ValueBuilder add(SingleStat stat) {
        return new ValueBuilder(stat);
    }

    public Stats(Stats old) {
        old.values.entrySet().stream()
                .filter(e -> e.getKey().getStat().getType().isPersistent())
                .forEach(e -> values.put(e.getKey(), e.getValue()));
    }

    public String toPrometheusString() {
        return values.entrySet().stream()
                .map(e -> e.getKey().toString() + " " + String.valueOf(e.getValue()))
                .sorted()
                .collect(Collectors.joining("\n"))+"\n";
    }

    /**
     * Merge updates all stat values from the provided `other` object. It will also remove
     * all non-persistent values that refer to any `SingleStat` type that is part of the
     * update coming from `other`.
     *
     * The idea is that each piece of code using merge to submit stats to the central
     * object updates only its own area of responsibility.
     *
     * @param other Stats to publish to this object
     * @return this object (with updated stats)
     */
    public Stats merge(Stats other) {
        // Find out which stats (ignore labels) are updated by this merge
        // but ignore persistent stats (whose we always want to keep)
        Set<SingleStat> refreshFor = other.values.keySet().stream()
                .map(SingleStatWLabels::getStat)
                .distinct()
                .filter(stat -> !stat.getType().isPersistent())
                .collect(Collectors.toSet());

        // Clear updated nonpersistent stats
        values.entrySet().removeIf(entry -> refreshFor.contains(entry.getKey().getStat()));

        // Publish updates
        for (Map.Entry<SingleStatWLabels, Double> entry: other.values.entrySet()) {
            if (entry.getKey().getStat().getType().isPersistent()) {
                values.put(entry.getKey(), values.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
            } else {
                values.put(entry.getKey(), entry.getValue());
            }
        }

        return this;
    }
}
