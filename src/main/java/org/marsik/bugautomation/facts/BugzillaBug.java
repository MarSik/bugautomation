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

    @NotNull
    String status;
    LocalDateTime statusModifiedAt;

    @NotNull
    BugzillaPriorityLevel priority;
    @NotNull
    BugzillaPriorityLevel severity;

    @NotNull
    String targetMilestone;
    String targetRelease;

    Set<String> verified;

    @NotNull
    Set<BugzillaBugFlag> flags;
    @NotNull
    Set<String> keywords;
    @NotNull
    Set<String> blocks;
    @NotNull
    User assignedTo;
    @NotNull
    Bug bug;
}
