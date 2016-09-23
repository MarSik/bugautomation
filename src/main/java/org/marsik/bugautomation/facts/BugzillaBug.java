package org.marsik.bugautomation.facts;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "id")
@Builder
public class BugzillaBug implements AssignmentTarget {
    String id;

    String title;
    String description;
    String status;

    Bug bug;
}
