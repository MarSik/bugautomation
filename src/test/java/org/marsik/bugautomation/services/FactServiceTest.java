package org.marsik.bugautomation.services;

import java.util.HashSet;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.KieServices;
import org.kie.api.cdi.KSession;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.marsik.bugautomation.cdi.WeldJUnit4Runner;
import org.marsik.bugautomation.facts.Bug;
import org.marsik.bugautomation.facts.BugzillaBug;
import org.marsik.bugautomation.facts.TrelloBoard;
import org.marsik.bugautomation.facts.TrelloCard;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(WeldJUnit4Runner.class)
public class FactServiceTest {
    @Inject
    @KSession("bug-rules")
    KieSession kSession;

    @Mock
    BugzillaActions bugzillaActions;

    @Mock
    TrelloActions trelloActions;

    @Mock
    ConfigurationService configurationService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void trigger() {
        kSession.setGlobal("bugzilla", bugzillaActions);
        kSession.setGlobal("trello", trelloActions);
        kSession.setGlobal("config", configurationService);
        kSession.fireAllRules();
    }

    @Test
    public void testOrderWithAndWithoutRelease() throws Exception {
        BugzillaBug bug1 = BugzillaBug.builder()
                .id("1")
                .targetMilestone(null)
                .build();

        BugzillaBug bug2 = BugzillaBug.builder()
                .id("2")
                .targetMilestone("ovirt-2.0.0")
                .bug(Bug.builder().id(1).build())
                .build();

        TrelloBoard board = TrelloBoard.builder()
                .id("sprint")
                .name("Sprint")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .board(board)
                .status("todo")
                .pos(1.0)
                .bug(bug1.getBug())
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("a")
                .board(board)
                .status("todo")
                .pos(2.0)
                .bug(bug2.getBug())
                .build();

        kSession.insert(bug1);
        kSession.insert(bug2);
        kSession.insert(board);
        kSession.insert(card1);
        kSession.insert(card2);

        kSession.fireAllRules();
    }
}
