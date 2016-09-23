package org.marsik.bugautomation.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.marsik.bugautomation.facts.Bug;

public class BugMatchingServiceTest {
    BugMatchingService bugs = new BugMatchingService();

    @Test
    public void testBugUrl() throws Exception {
        assertThat(bugs.identifyBug("https://bugzilla.redhat.com/show_bug.cgi?id=1378310").get())
                .isNotNull();
    }

    @Test
    public void testShortBugUrl() throws Exception {
        assertThat(bugs.identifyBug("https://bugzilla.redhat.com/1378310").get())
                .isNotNull();
    }

    @Test
    public void testBugId() throws Exception {
        assertThat(bugs.identifyBug("[1378310]").get())
                .isNotNull();
    }

    @Test
    public void testPlainBugId() throws Exception {
        assertThat(bugs.identifyBug("1378310").get())
                .isNotNull();
    }

    @Test
    public void testStringBugId() throws Exception {
        assertThat(bugs.identifyBug("bug 1378310").get())
                .isNotNull();
    }

    @Test
    public void testStringBugIdNoSpace() throws Exception {
        assertThat(bugs.identifyBug("bug1378310").get())
                .isNotNull();
    }

    @Test
    public void testHashBugId() throws Exception {
        assertThat(bugs.identifyBug("#1378310").get())
                .isNotNull();
    }

    @Test
    public void testBzHashBugId() throws Exception {
        assertThat(bugs.identifyBug("bz#1378310").get())
                .isNotNull();
    }

    @Test
    public void testBugHashBugId() throws Exception {
        assertThat(bugs.identifyBug("bug#1378310").get())
                .isNotNull();
    }

    @Test
    public void testRhbzHashBugId() throws Exception {
        assertThat(bugs.identifyBug("rhbz#1378310").get())
                .isNotNull();
    }

    @Test
    public void testRhbzBugId() throws Exception {
        assertThat(bugs.identifyBug("rhbz1378310").get())
                .isNotNull();
    }

    @Test
    public void testBugIdMatching() throws Exception {
        final Bug rhbz1378310 = bugs.identifyBug("rhbz1378310").get();
        final Bug bug1378310 = bugs.identifyBug("bug#1378310").get();

        assertThat(rhbz1378310)
                .isNotNull()
                .isEqualTo(bug1378310);
    }
}
