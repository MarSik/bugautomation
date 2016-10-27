package org.marsik.bugautomation.facts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.marsik.bugautomation.facts.BugzillaStatus.*;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;

public class BugzillaBugTest {
    @Test
    public void isDone() throws Exception {
        assertBugStatusIsDone(NEW).isFalse();
        assertBugStatusIsDone(ASSIGNED).isFalse();
        assertBugStatusIsDone(POST).isFalse();
        assertBugStatusIsDone(MODIFIED).isTrue();
        assertBugStatusIsDone(ON_QA).isTrue();
        assertBugStatusIsDone(VERIFIED).isTrue();
        assertBugStatusIsDone(CLOSED).isTrue();
    }

    private AbstractBooleanAssert<?> assertBugStatusIsDone(BugzillaStatus status) {
        BugzillaBug bz = BugzillaBug.builder()
                .status(status)
                .build();

        return assertThat(bz.isDone());
    }
}
