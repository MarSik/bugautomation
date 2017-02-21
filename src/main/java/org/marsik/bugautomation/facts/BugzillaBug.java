package org.marsik.bugautomation.facts;

import java.time.LocalDateTime;
import java.util.Set;

import javax.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "id")
@Builder
public class BugzillaBug {
    String id;

    String title;
    String description;

    /**
     * What community does this bug belong to? oVirt, Red Hat or other?
     */
    @NotNull
    String community;

    @NotNull
    BugzillaStatus status;
    LocalDateTime statusModifiedAt;

    @NotNull
    BugzillaPriorityLevel priority;
    @NotNull
    BugzillaPriorityLevel severity;

    /**
     * PM score - higher numbers have to be finished first
     */
    @NotNull
    Integer pmScore;

    /**
     * PM priority - lower numbers have to be finished first
     */
    @NotNull
    Integer pmPriority;

    @NotNull
    String targetMilestone;
    String targetRelease;

    Set<String> verified;

    @NotNull
    Set<BugzillaBugFlag> flags;
    @NotNull
    Set<String> keywords;
    @NotNull
    Set<Bug> blocks;
    @NotNull
    User assignedTo;
    @NotNull
    Bug bug;

    public boolean isDone() {
        return BugzillaStatus.MODIFIED.compareTo(status) <= 0;
    }

    public boolean isUntargeted() {
        return targetMilestone == null || targetMilestone.isEmpty();
    }

    public boolean isTargeted() {
        return !isUntargeted();
    }
}
