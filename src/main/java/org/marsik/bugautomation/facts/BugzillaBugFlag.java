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

    /**
     * This method checks whether this flag is an approval flag for given targetRelease
     *
     * That usually means it has to start with ovirt|rhevm, contain the version with possibly
     * the last digit replaced by .z and be granted (+).
     *
     * @param targetRelease
     * @return true if the flag approves the given release
     */
    public boolean approves(String targetRelease) {
        return flag.equals(targetRelease + "+") // ovirt-X.Y.Z+
                || flag.equals(targetRelease.replace("ovirt", "rhevm") + "+") // rhevm-X.Y.Z+
                || flag.equals(targetRelease.replaceAll("\\.[0-9]+$", ".z") + "+") // ovirt-X.Y.z+
                || flag.equals(targetRelease.replace("ovirt", "rhevm").replaceAll("\\.[0-9]+$", ".z") + "+") // rhevm-X.Y.z+
                || flag.equals(targetRelease.replaceAll("\\.[0-9]+$", "") + "+") // ovirt-X.Y+
                || flag.equals(targetRelease.replace("ovirt", "rhevm").replaceAll("\\.[0-9]+$", "") + "+") // rhevm-X.Y+
                || flag.equals(targetRelease.replaceAll("\\.[0-9]+$", "-ga") + "+") // ovirt-X.Y-ga+
                || flag.equals(targetRelease.replace("ovirt", "rhevm").replaceAll("\\.[0-9]+$", "-ga") + "+"); // rhevm-X.Y-ga+
    }

    /**
     * This method checks whether a flags marks a bug as a future RFE
     * @return true if the flag is a future flag
     */
    public boolean futureFlag() {
        return flag.matches(".*-future[?+]");
    }
}
