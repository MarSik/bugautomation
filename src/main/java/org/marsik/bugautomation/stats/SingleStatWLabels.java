package org.marsik.bugautomation.stats;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SingleStatWLabels {
    @NotNull
    private SingleStat stat;
    private Set<LabelValue> labels = new HashSet<>();

    public String toString() {
        String strLabels = labels.stream()
                .map(v -> v.getName() + "=\"" + v.getSafeValue() + "\"")
                .sorted()
                .collect(Collectors.joining(","));

        if (!strLabels.isEmpty()) {
            strLabels = "{" +strLabels+ "}";
        }

        return stat.getName() + strLabels;
    }
}
