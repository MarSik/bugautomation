package org.marsik.bugautomation.facts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TrelloCardTest {
    @Test
    public void testGetCleanDesc() throws Exception {
        TrelloCard kiCard = TrelloCard.builder()
                .description("Test {{ id=5}} with values {{ bug=1234567 }}")
                .build();

        assertThat(kiCard.getCleanDesc())
                .isEqualTo("Test   with values  ");
    }
}
