package org.marsik.bugautomation.facts;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Assignment {
    User user;
    AssignmentTarget target;
}
