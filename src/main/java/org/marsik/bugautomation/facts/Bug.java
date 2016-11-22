package org.marsik.bugautomation.facts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
public class Bug {
    String id;
    IdType type;

    public Bug(String id) {
        this(id, IdType.REAL);
    }

    public enum IdType {
        REAL, CUSTOM
    }

    public boolean isReal() {
        return type == IdType.REAL;
    }
}
