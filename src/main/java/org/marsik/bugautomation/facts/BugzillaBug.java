package org.marsik.bugautomation.facts;

import java.time.LocalDateTime;
import java.util.Set;

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

    String status;
    LocalDateTime statusModifiedAt;

    BugzillaPriorityLevel priority;
    BugzillaPriorityLevel severity;

    String targetMilestone;
    String targetRelease;

    Boolean blocker;
    Set<String> verified;

    Set<BugzillaBugFlag> flags;
    Set<String> keywords;

    Set<String> blocks;

    User assignedTo;

    Bug bug;
}
