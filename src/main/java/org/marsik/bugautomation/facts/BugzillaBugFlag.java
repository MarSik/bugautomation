package org.marsik.bugautomation.facts;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "name")
public class BugzillaBugFlag {
    private final String name;
    private final String value;
    private final LocalDateTime modifiedAt;

    public BugzillaBugFlag(Map<String, Object> flag) {
        name = (String)flag.get("name");
        value = (String)flag.get("status");
        modifiedAt = LocalDateTime.ofInstant(((Date)flag.get("modification_date")).toInstant(), ZoneId.of("UTC"));
    }
}
