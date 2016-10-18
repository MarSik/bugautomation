package org.marsik.bugautomation.stats;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class StatsTest {
    @Test
    public void testCounter() throws Exception {
        Stats stats = new Stats();
        stats.add(SingleStat.TRIGGER_COUNT)
                .value(5f);

        stats = new Stats(stats);

        stats.add(SingleStat.TRIGGER_COUNT)
                .value(5f);

        String result = stats.toPrometheusString();
        assertThat(result)
                .isEqualTo("bug_automation_trigger_count 10.0\n");
    }

    @Test
    public void testTrivial() throws Exception {
        Stats stats = new Stats();
        stats.add(SingleStat.SPRINT_CONTENT)
                .value(5f);

        stats.add(SingleStat.SPRINT_CONTENT)
                .value(5f);

        String result = stats.toPrometheusString();
        assertThat(result)
                .isEqualTo("bug_automation_sprint_content 10.0\n");
    }

    @Test
    public void testSimple() throws Exception {
        Stats stats = new Stats();
        stats.add(SingleStat.SPRINT_CONTENT)
                .label("host", "one")
                .label("status", "done")
                .value(5f);

        stats.add(SingleStat.SPRINT_CONTENT)
                .label("status", "done")
                .label("host", "one")
                .value(5f);

        String result = stats.toPrometheusString();
        assertThat(result)
                .isEqualTo("bug_automation_sprint_content{host=\"one\",status=\"done\"} 10.0\n");
    }

    @Test
    public void testTwo() throws Exception {
        Stats stats = new Stats();
        stats.add(SingleStat.SPRINT_CONTENT)
                .label("host", "one")
                .label("status", "done")
                .value(5f);

        stats.add(SingleStat.SPRINT_CONTENT)
                .label("status", "in_progress")
                .label("host", "one")
                .value(5f);

        String result = stats.toPrometheusString();
        assertThat(result)
                .isEqualTo("bug_automation_sprint_content{host=\"one\",status=\"done\"} 5.0\n" +
                        "bug_automation_sprint_content{host=\"one\",status=\"in_progress\"} 5.0\n");
    }
}
