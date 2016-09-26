package org.marsik.bugautomation.facts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class BugzillaPriorityLevelTest {
    @Test
    public void testPriorityOrdering() throws Exception {
        assertThat(BugzillaPriorityLevel.URGENT)
                .isGreaterThan(BugzillaPriorityLevel.HIGH);

        assertThat(BugzillaPriorityLevel.HIGH)
                .isGreaterThan(BugzillaPriorityLevel.MEDIUM);

        assertThat(BugzillaPriorityLevel.MEDIUM)
                .isGreaterThan(BugzillaPriorityLevel.LOW);

        assertThat(BugzillaPriorityLevel.LOW)
                .isGreaterThan(BugzillaPriorityLevel.UNSPECIFIED);
    }
}
