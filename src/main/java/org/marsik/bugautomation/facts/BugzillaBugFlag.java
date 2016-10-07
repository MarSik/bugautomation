package org.marsik.bugautomation.facts;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "flag")
public class BugzillaBugFlag {
    private final String flag;
    private final LocalDateTime modifiedAt;

    public BugzillaBugFlag(Map<String, Object> flag) {
        this.flag = (String)flag.get("name") + flag.get("status");
        modifiedAt = LocalDateTime.ofInstant(((Date)flag.get("modification_date")).toInstant(), ZoneId.of("UTC"));
    }

    public BugzillaBugFlag(final String flag) {
        this.flag = flag;
        modifiedAt = LocalDateTime.now();
    }
}
