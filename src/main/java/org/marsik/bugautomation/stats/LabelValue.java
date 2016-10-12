package org.marsik.bugautomation.stats;

import com.google.common.collect.ComparisonChain;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@AllArgsConstructor
@EqualsAndHashCode
public class LabelValue implements Comparable<LabelValue> {
    String name;
    String value;

    @Override
    public int compareTo(LabelValue other) {
        return ComparisonChain.start()
                .compare(this.name, other.name)
                .compare(this.value, other.value)
                .result();
    }
}
