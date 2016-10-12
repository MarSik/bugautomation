package org.marsik.bugautomation.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Stats {
    Map<SingleStatWLabels, Float> values = new HashMap<>();

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

        public void value(Float value) {
            SingleStatWLabels statKey = SingleStatWLabels.builder()
                    .stat(base)
                    .build();

            labels.forEach((k, v) -> statKey.getLabels().add(new LabelValue(k, v)));
            values.putIfAbsent(statKey, 0f);
            Float v = values.get(statKey) + value;
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
                .collect(Collectors.joining("\n"));
    }
}
