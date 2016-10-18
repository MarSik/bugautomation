package org.marsik.bugautomation.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;


public class TrelloRefreshJobTest {
    @Test
    public void empty() throws Exception {
        String testDoc = "Test description with suffix";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void noValues() throws Exception {
        String testDoc = "Test description {{}} with suffix";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void multipleNoValues() throws Exception {
        String testDoc = "Test description {{}} with {{}} suffix";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void simple() throws Exception {
        String testDoc = "Test description {{ score=400 }} with suffix";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .hasSize(1);

        assertThat(values.get("score"))
                .isNotNull()
                .isEqualTo("400");
    }

    @Test
    public void multipleSame() throws Exception {
        String testDoc = "Test description {{ score=400 }} with suffix {{ score=300 }}";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .hasSize(1);

        assertThat(values.get("score"))
                .isNotNull()
                .isEqualTo("300");
    }

    @Test
    public void multipleDiff() throws Exception {
        String testDoc = "Test description {{ score=400 }} with suffix {{ score2=300 }}";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .hasSize(2);

        assertThat(values.get("score"))
                .isNotNull()
                .isEqualTo("400");

        assertThat(values.get("score2"))
                .isNotNull()
                .isEqualTo("300");
    }

    @Test
    public void complex() throws Exception {
        String testDoc = "Test description {{ score=400   test=mail@admin.cz}} with suffix {{score2=300}}";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .hasSize(3);

        assertThat(values.get("score"))
                .isNotNull()
                .isEqualTo("400");

        assertThat(values.get("score2"))
                .isNotNull()
                .isEqualTo("300");

        assertThat(values.get("test"))
                .isNotNull()
                .isEqualTo("mail@admin.cz");

    }
}
