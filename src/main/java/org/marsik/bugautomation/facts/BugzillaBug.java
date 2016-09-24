package org.marsik.bugautomation.facts;

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

    BugzillaPriorityLevel priority;
    BugzillaPriorityLevel severity;

    User assignedTo;

    Bug bug;
}
