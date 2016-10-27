package org.marsik.bugautomation.facts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.marsik.bugautomation.facts.BugzillaStatus.*;

import org.junit.Test;

public class BugzillaStatusTest {
    @Test
    public void testStatusOrder() throws Exception {
        assertThat(UNDEFINED)
                .isLessThan(NEW);
        assertThat(NEW)
                .isLessThan(ASSIGNED);
        assertThat(ASSIGNED)
                .isLessThan(POST);
        assertThat(POST)
                .isLessThan(MODIFIED);
        assertThat(MODIFIED)
                .isLessThan(ON_QA);
        assertThat(ON_QA)
                .isLessThan(VERIFIED);
        assertThat(VERIFIED)
                .isLessThan(CLOSED);
    }



}
