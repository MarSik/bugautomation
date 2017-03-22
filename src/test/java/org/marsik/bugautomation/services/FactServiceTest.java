package org.marsik.bugautomation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.cdi.KSession;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.runtime.KieSession;
import org.marsik.bugautomation.cdi.WeldJUnit4Runner;
import org.marsik.bugautomation.facts.Bug;
import org.marsik.bugautomation.facts.BugzillaBug;
import org.marsik.bugautomation.facts.BugzillaBugFlag;
import org.marsik.bugautomation.facts.BugzillaPriorityLevel;
import org.marsik.bugautomation.facts.BugzillaStatus;
import org.marsik.bugautomation.facts.TrelloBoard;
import org.marsik.bugautomation.facts.TrelloCard;
import org.marsik.bugautomation.facts.TrelloLabel;
import org.marsik.bugautomation.facts.User;
import org.marsik.bugautomation.stats.Stats;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

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

    @Inject
    InternalActions internalActions;

    @Inject
    FactService factService;

    private static final String TRELLO_BACKLOG = "todo";

    private TrelloBoard board;
    private User user = new User("test");

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(configurationService.getCached("trello.boards")).thenReturn("sprint");
        when(configurationService.getCached("cfg.backlog.sprint")).thenReturn(TRELLO_BACKLOG);
        when(configurationService.getCached("cfg.done.sprint")).thenReturn("done");
        when(configurationService.getCached("release.future.prefix")).thenReturn("ovirt-4.1.");
        when(configurationService.getCached("release.future.release")).thenReturn("ovirt-4.1.0");
        when(configurationService.isBoardMonitored("sprint")).thenReturn(true);
        when(configurationService.getBacklog(any())).thenReturn(TRELLO_BACKLOG);
        when(configurationService.getDonelog(any())).thenReturn("done");

        when(configurationService.getCachedInt("release.ovirt-4.0.6", 0)).thenReturn(200);

        board = TrelloBoard.builder()
                .id("sprint")
                .name("Sprint")
                .build();

        // Clear session and populate with board fact
        factService.clear();
        factService.addFact(board);
    }

    private void trigger() {
        kSession.setGlobal("internal", internalActions);
        kSession.setGlobal("bugzilla", bugzillaActions);
        kSession.setGlobal("trello", trelloActions);
        kSession.setGlobal("config", configurationService);
        kSession.insert(new Stats());
        kSession.fireAllRules();
    }

    private BugzillaBug.BugzillaBugBuilder newBug(Integer id, BugzillaStatus status) {
        return BugzillaBug.builder()
                .id(id.toString())
                .targetMilestone("")
                .priority(BugzillaPriorityLevel.UNSPECIFIED)
                .severity(BugzillaPriorityLevel.UNSPECIFIED)
                .bug(new Bug(String.valueOf(id)))
                .priority(BugzillaPriorityLevel.UNSPECIFIED)
                .status(status)
                .pmScore(0)
                .pmPriority(Integer.MAX_VALUE)
                .assignedTo(user);
    }

    private TrelloCard.TrelloCardBuilder cardForBug(BugzillaBug bug, Double pos) {
        return TrelloCard.builder()
                .id("card-"+bug.getId())
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(pos)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .bug(bug.getBug());
    }

    @Test
    public void testOrderWithAndWithoutRelease() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .targetMilestone("ovirt-4.0.6")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(2.0)
                .bug(bug2.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card1.getScore())
                .isLessThan(card2.getScore());
    }

    @Test
    public void testDoneBugNoFlags() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.MODIFIED)
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).moveCard(card1, board, "documentation");
    }

    @Test
    public void testCardArtificialId() throws Exception {
        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(new Bug("test-card"))
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(card1);
        factService.addFact(card1.getBug());

        trigger();

        verify(trelloActions, never()).moveCard(card1, board, "documentation");
        verify(trelloActions, never()).moveCard(card1, board, "done");
    }

    @Test
    public void testCardRealIdNoBugPresent() throws Exception {
        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(new Bug("1234567"))
                .blocks(Collections.emptySet())
                .labels(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(card1);

        trigger();

        verify(trelloActions).moveCard(card1, board, "done");
        assertThat(card1.getLabels()).isEmpty();
    }

    @Test
    public void testDoneOldNoMove() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.CLOSED)
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status("done before X")
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions, never()).moveCard(card1, board, "done");
    }

    @Test
    public void testDoneOldReopenedMove() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status("done before X")
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).moveCard(card1, board, TRELLO_BACKLOG);
    }

    @Test
    public void testArchivedReopenedMove() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .board(board)
                .status("done before X")
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .closed(true)
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions, atLeastOnce()).moveCard(card1, board, TRELLO_BACKLOG);
    }

    @Test
    public void testArchivedInTodoUnarchive() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .board(board)
                .status("todo")
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .closed(true)
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).moveCard(card1, board, TRELLO_BACKLOG);
    }

    @Test
    public void testDoneBugNoDocFlag() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.MODIFIED)
                .flags(Collections.singleton(new BugzillaBugFlag("requires_doc_text-")))
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).moveCard(card1, board, "done");
    }

    @Test
    public void testNeedsTriage() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .bug(bug1.getBug())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).assignLabelToCard(card1, "triage");
    }

    @Test
    public void testCardNeedsTriage() throws Exception {
        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();


        factService.addFact(card1);

        trigger();

        verify(trelloActions).assignLabelToCard(card1, "triage");
    }

    @Test
    public void testCardCustomIdNeedsTriage() throws Exception {
        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .bug(new Bug("1"))
                .build();

        factService.addFact(card1.getBug());
        factService.addFact(card1);

        trigger();

        verify(trelloActions).assignLabelToCard(card1, "triage");
    }

    @Test
    public void testNoNeedTriageDone() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.MODIFIED)
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .bug(bug1.getBug())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card1, "triage");
    }

    @Test
    public void testNoNeedTriageCardDone() throws Exception {
        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status("done before 25th")
                .pos(1.0)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .bug(new Bug("1"))
                .build();

        factService.addFact(card1.getBug());
        factService.addFact(card1);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card1, "triage");
    }

    @Test
    public void testNoNeedTriageFuture() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .keywords(singletonSet("futurefeature"))
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card1, "triage");
    }

    @Test
    public void testRemoveTriageDocumentation() throws Exception {
        TrelloLabel label = TrelloLabel.builder()
                .name("triage")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status("documentation")
                .labels(singletonSet(label))
                .pos(1.0)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();


        factService.addFact(card1);

        trigger();

        verify(trelloActions).removeLabelFromCard(card1, label);
    }

    @Test
    public void testRemoveTriageDone() throws Exception {
        TrelloLabel label = TrelloLabel.builder()
                .name("triage")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status("done")
                .labels(singletonSet(label))
                .pos(1.0)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();


        factService.addFact(card1);

        trigger();

        verify(trelloActions).removeLabelFromCard(card1, label);
    }

    @Test
    public void testRemoveTriageFuture() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.MODIFIED)
                .keywords(singletonSet("futurefeature"))
                .build();

        TrelloLabel label = TrelloLabel.builder()
                .name("triage")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .labels(singletonSet(label))
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).removeLabelFromCard(card1, label);
    }

    @Test
    public void testRemoveTriageFutureFlag() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .flags(singletonSet(new BugzillaBugFlag("ovirt-future?")))
                .build();

        TrelloLabel label = TrelloLabel.builder()
                .name("triage")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .labels(singletonSet(label))
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).removeLabelFromCard(card1, label);
    }

    @Test
    public void testRemoveTriageDueDateg() throws Exception {
        TrelloLabel label = TrelloLabel.builder()
                .name("triage")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .labels(singletonSet(label))
                .dueDate(Instant.now())
                .blocks(new HashSet<>())
                .assignedTo(new HashSet<>())
                .pos(1.0)
                .build();

        factService.addFact(card1);

        trigger();

        verify(trelloActions).removeLabelFromCard(card1, label);
    }

    @Test
    public void testRemoveTriageFutureFlagRhv() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .flags(singletonSet(new BugzillaBugFlag("rhevm-future?")))
                .build();

        TrelloLabel label = TrelloLabel.builder()
                .name("triage")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .labels(singletonSet(label))
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).removeLabelFromCard(card1, label);
    }

    @Test
    public void testFlagsNotNeededRelease() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.MODIFIED)
                .community("redhat")
                .build();

        final TrelloLabel label = TrelloLabel.builder()
                .id("000")
                .name("flags missing")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .labels(singletonSet(label))
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).removeLabelFromCard(card1, label);
    }

    @Test
    public void testFlagsNotNeededCommunity() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.MODIFIED)
                .targetMilestone("ovirt-4.0.6")
                .community("ovirt")
                .build();

        final TrelloLabel label = TrelloLabel.builder()
                .id("000")
                .name("flags missing")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .labels(singletonSet(label))
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).removeLabelFromCard(card1, label);
    }

    @Test
    public void testFlagsNeeded() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.MODIFIED)
                .targetMilestone("ovirt-4.0.6")
                .community("redhat")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .labels(new HashSet<>())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).assignLabelToCard(card1, "flags missing");
    }

    @Test
    public void testOrderWithBlockingBug() throws Exception {
        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .targetMilestone("ovirt-4.0.6")
                .build();

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug2.getBug()))
                .build();



        TrelloCard card1 = cardForBug(bug1, 2.0)
                .build();

        TrelloCard card2 = cardForBug(bug2, 1.0)
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card2.getScore())
                .isNotNull()
                .isEqualTo(card1.getScore());
        verify(trelloActions).switchCards(card2, card1);
    }

    @Test
    public void testOrderWithScore() throws Exception {
        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .build();

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();



        TrelloCard card1 = cardForBug(bug1, 1.0)
                .score(100)
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0)
                .score(200)
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card2.getScore())
                .isNotNull()
                .isGreaterThan(card1.getScore());
        verify(trelloActions).switchCards(card1, card2);
    }

    @Test
    public void testOrderWithPmScore() throws Exception {
        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .pmScore(10)
                .build();

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .pmScore(100)
                .build();



        TrelloCard card1 = cardForBug(bug1, 2.0)
                .build();

        TrelloCard card2 = cardForBug(bug2, 1.0)
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card2.getScore())
                .isNotNull()
                .isEqualTo(card1.getScore());
        verify(trelloActions).switchCards(card2, card1);
    }

    @Test
    public void testOrderWithPmPriority() throws Exception {
        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .pmPriority(2)
                .build();

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .pmPriority(1)
                .build();



        TrelloCard card1 = cardForBug(bug1, 2.0)
                .build();

        TrelloCard card2 = cardForBug(bug2, 1.0)
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card2.getScore())
                .isNotNull()
                .isEqualTo(card1.getScore());
        verify(trelloActions).switchCards(card2, card1);
    }

    @Test
    public void testOrderWithPmPriorityAndBlocking() throws Exception {
        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .pmPriority(1)
                .build();

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .pmPriority(2)
                .blocks(singletonSet(bug2.getBug()))
                .build();



        TrelloCard card1 = cardForBug(bug1, 2.0)
                .build();

        TrelloCard card2 = cardForBug(bug2, 1.0)
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card2.getScore())
                .isNotNull()
                .isEqualTo(card1.getScore());
        verify(trelloActions).switchCards(card2, card1);
    }

    @Test
    public void testOrderWithPmScoreAndBlocking() throws Exception {
        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .pmScore(1000)
                .build();

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug2.getBug()))
                .build();



        TrelloCard card1 = cardForBug(bug1, 2.0)
                .build();

        TrelloCard card2 = cardForBug(bug2, 1.0)
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card2.getScore())
                .isNotNull()
                .isEqualTo(card1.getScore());
        verify(trelloActions).switchCards(card2, card1);
    }

    @Test
    public void testOrderWithPmScoreAndPriority() throws Exception {
        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .pmScore(1000)
                .pmPriority(2)
                .build();

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .pmScore(100)
                .pmPriority(1)
                .build();



        TrelloCard card1 = cardForBug(bug1, 2.0)
                .build();

        TrelloCard card2 = cardForBug(bug2, 1.0)
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card2.getScore())
                .isNotNull()
                .isEqualTo(card1.getScore());
        verify(trelloActions).switchCards(card2, card1);
    }

    @Test
    public void testScoreWithBlockingBug() throws Exception {
        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .targetMilestone("ovirt-4.0.6")
                .build();

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug2.getBug()))
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(2.0)
                .bug(bug2.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card2.getScore())
                .isNotNull()
                .isEqualTo(card1.getScore());
    }

    @Test
    public void testOrderWithBlockingCard() throws Exception {
        Bug bug = new Bug("1");

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .bug(bug)
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(2.0)
                .score(100)
                .blocks(singletonSet(bug))
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card2.getScore())
                .isNotNull()
                .isEqualTo(card1.getScore());

        verify(trelloActions).switchCards(card1, card2);
    }

    @Test
    public void testWaitingWithBlockingCard() throws Exception {
        Bug bug = new Bug("1");

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .bug(bug)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(2.0)
                .score(100)
                .blocks(singletonSet(bug))
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions).assignLabelToCard(card1, "waiting");
    }

    @Test
    public void testWaitingWithBlockingCardCustomId() throws Exception {
        Bug bug = new Bug("test-custom-id");

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .bug(bug)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status("inprogress")
                .pos(2.0)
                .score(100)
                .blocks(singletonSet(bug))
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(bug);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions).assignLabelToCard(card1, "waiting");
    }

    @Test
    public void testNoWaitingWithBlockingCardDone() throws Exception {
        Bug bug = new Bug("1");

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .bug(bug)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status("done")
                .pos(2.0)
                .score(100)
                .blocks(singletonSet(bug))
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card1, "waiting");
    }

    @Test
    public void testNoWaitingWithBlockingCardInDocumentation() throws Exception {
        Bug bug = new Bug("1");

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .bug(bug)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status("documentation")
                .pos(2.0)
                .score(100)
                .blocks(singletonSet(bug))
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card1, "waiting");
    }

    @Test
    public void testRemoveWaitingWithBlockingCardDone() throws Exception {
        Bug bug = new Bug("1");

        final TrelloLabel waitingLabel = TrelloLabel.builder().name("waiting").build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .bug(bug)
                .labels(singletonSet(waitingLabel))
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status("done")
                .pos(2.0)
                .score(100)
                .blocks(singletonSet(bug))
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions, atLeastOnce()).removeLabelFromCard(card1, waitingLabel);
    }

    @Test
    public void testRemoveWaitingWithBlockingCardInDocumentation() throws Exception {
        Bug bug = new Bug("1");

        final TrelloLabel waitingLabel = TrelloLabel.builder().name("waiting").build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .bug(bug)
                .labels(singletonSet(waitingLabel))
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status("documentation")
                .pos(2.0)
                .score(100)
                .blocks(singletonSet(bug))
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions, atLeastOnce()).removeLabelFromCard(card1, waitingLabel);
    }

    @Test
    public void testWaitingWithBlockingBug() throws Exception {
        Bug bug = new Bug("1");

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .bug(bug)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug))
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0).build();

        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions).assignLabelToCard(card1, "waiting");
    }

    @Test
    public void testNoWaitingWithBlockingBugDoneInTrello() throws Exception {
        Bug bug = new Bug("1");

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .bug(bug)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug))
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0)
                .status("done")
                .build();

        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card1, "waiting");
    }

    @Test
    public void testNoWaitingWithBlockingBugDoneInDocumentation() throws Exception {
        Bug bug = new Bug("1");

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .bug(bug)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug))
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0)
                .status("documentation")
                .build();

        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card1, "waiting");
    }

    @Test
    public void testRemoveWithBlockingBugDoneInTrello() throws Exception {
        Bug bug = new Bug("1");

        final TrelloLabel waitingLabel = TrelloLabel.builder().name("waiting").build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .bug(bug)
                .blocks(Collections.emptySet())
                .labels(singletonSet(waitingLabel))
                .assignedTo(new HashSet<>())
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug))
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0)
                .status("done")
                .build();

        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions).removeLabelFromCard(card1, waitingLabel);
    }

    @Test
    public void testRemoveWithBlockingBugInDocumentationInTrello() throws Exception {
        Bug bug = new Bug("1");

        final TrelloLabel waitingLabel = TrelloLabel.builder().name("waiting").build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .bug(bug)
                .blocks(Collections.emptySet())
                .labels(singletonSet(waitingLabel))
                .assignedTo(new HashSet<>())
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug))
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0)
                .status("documentation")
                .build();

        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions).removeLabelFromCard(card1, waitingLabel);
    }

    @Test
    public void testNoWaitingBugWithBlockingBugDoneInTrello() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        TrelloCard card1 = cardForBug(bug1, 1.0)
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug1.getBug()))
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0)
                .status("done")
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card1, "waiting");
    }

    @Test
    public void testNoWaitingBugWithBlockingBugDoneButCardNot() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        TrelloCard card1 = cardForBug(bug1, 1.0)
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.MODIFIED)
                .blocks(singletonSet(bug1.getBug()))
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0)
                .status("inprogress")
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card1, "waiting");
    }

    @Test
    public void testNoWaitingBugWithBlockingBugInDocumentationInTrello() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        TrelloCard card1 = cardForBug(bug1, 1.0)
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug1.getBug()))
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0)
                .status("documentation")
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card1, "waiting");
    }

    @Test
    public void testRemoveBugWithBlockingBugDoneInTrello() throws Exception {
        final TrelloLabel waitingLabel = TrelloLabel.builder().name("waiting").build();

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        TrelloCard card1 = cardForBug(bug1, 1.0)
                .labels(singletonSet(waitingLabel))
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug1.getBug()))
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0)
                .status("done")
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions).removeLabelFromCard(card1, waitingLabel);
    }

    @Test
    public void testNoRemoveBugWithNotAllBlockingBugsDoneInTrello() throws Exception {
        final TrelloLabel waitingLabel = TrelloLabel.builder().name("waiting").build();

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        TrelloCard card1 = cardForBug(bug1, 1.0)
                .labels(singletonSet(waitingLabel))
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug1.getBug()))
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0)
                .status("done")
                .build();

        BugzillaBug bug3 = newBug(3, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug1.getBug()))
                .build();

        TrelloCard card3 = cardForBug(bug3, 3.0)
                .status("inprogress")
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(bug3);
        factService.addFact(card1);
        factService.addFact(card2);
        factService.addFact(card3);

        trigger();

        verify(trelloActions, never()).removeLabelFromCard(card1, waitingLabel);
    }

    @Test
    public void testRemoveBugWithBlockingBugDoneButCardNot() throws Exception {
        final TrelloLabel waitingLabel = TrelloLabel.builder().name("waiting").build();

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        TrelloCard card1 = cardForBug(bug1, 1.0)
                .labels(singletonSet(waitingLabel))
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.MODIFIED)
                .blocks(singletonSet(bug1.getBug()))
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0)
                .status("inprogress")
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions, atLeastOnce()).removeLabelFromCard(card1, waitingLabel);
    }

    @Test
    public void testRemoveBugWithBlockingBugInDocumentationInTrello() throws Exception {
        final TrelloLabel waitingLabel = TrelloLabel.builder().name("waiting").build();

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        TrelloCard card1 = cardForBug(bug1, 1.0)
                .labels(singletonSet(waitingLabel))
                .build();

        BugzillaBug bug2 = newBug(2, BugzillaStatus.ASSIGNED)
                .blocks(singletonSet(bug1.getBug()))
                .build();

        TrelloCard card2 = cardForBug(bug2, 2.0)
                .status("documentation")
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions).removeLabelFromCard(card1, waitingLabel);
    }

    @Test
    public void testNoWaitingWithNoBlockingBugPresent() throws Exception {
        Bug bug = new Bug("1");

        final TrelloLabel waitingLabel = TrelloLabel.builder().name("waiting").build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .score(100)
                .bug(bug)
                .blocks(Collections.emptySet())
                .labels(singletonSet(waitingLabel))
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(card1);

        trigger();

        verify(trelloActions, atLeastOnce()).removeLabelFromCard(card1, waitingLabel);
    }

    @Test
    public void testScoreWithBlockingCard() throws Exception {
        Bug bug = new Bug("1");

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .blocks(singletonSet(bug))
                .assignedTo(new HashSet<>())
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(2.0)
                .bug(bug)
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .score(200)
                .build();

        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card2.getScore())
                .isNotNull()
                .isEqualTo(card1.getScore());
    }

    @Test
    public void testZStreamScore1() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "ovirt-4.0.6+"))
                .isNotNull()
                .isGreaterThan(100);
    }

    @Test
    public void testZStreamScore2() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "rhevm-4.0.6+"))
                .isNotNull()
                .isGreaterThan(100);
    }

    @Test
    public void testZStreamScore3() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "ovirt-4.0.z+"))
                .isNotNull()
                .isGreaterThan(100);
    }

    @Test
    public void testZStreamScore4() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "rhevm-4.0.z+"))
                .isNotNull()
                .isGreaterThan(100);
    }

    @Test
    public void testZStreamScore5() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "ovirt-4.0-ga+"))
                .isNotNull()
                .isGreaterThan(100);
    }

    @Test
    public void testZStreamScore6() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "rhevm-4.0-ga+"))
                .isNotNull()
                .isGreaterThan(100);
    }

    @Test
    public void testZStreamScore7() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "ovirt-4.0+"))
                .isNotNull()
                .isGreaterThan(100);
    }

    @Test
    public void testZStreamScore8() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "rhevm-4.0+"))
                .isNotNull()
                .isGreaterThan(100);
    }

    @Test
    public void testZStreamScore1neg() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "ovirt-4.0.6-"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore2neg() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "rhevm-4.0.6-"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore3neg() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "ovirt-4.0.z-"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore4neg() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "rhevm-4.0.z-"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore5neg() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "ovirt-4.0-ga-"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore6neg() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "rhevm-4.0-ga-"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore7neg() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "ovirt-4.0-"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore8neg() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "rhevm-4.0-"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore1waiting() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "ovirt-4.0.6?"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore2waiting() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "rhevm-4.0.6?"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore3waiting() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "ovirt-4.0.z?"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore4waiting() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "rhevm-4.0.z?"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore5waiting() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "ovirt-4.0-ga?"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore6waiting() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "rhevm-4.0-ga?"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore7waiting() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "ovirt-4.0?"))
                .isNotNull()
                .isLessThan(100);
    }

    @Test
    public void testZStreamScore8waiting() throws Exception {
        assertThat(performReleaseAndFlagTest("ovirt-4.0.6", "rhevm-4.0?"))
                .isNotNull()
                .isLessThan(100);
    }

    private Integer performReleaseAndFlagTest(String release, String flag) {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.MODIFIED)
                .targetMilestone(release)
                .community("redhat")
                .flags(singletonSet(new BugzillaBugFlag(flag)))
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .blocks(Collections.emptySet())
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        return card1.getScore();
    }

    @Test
    public void testIsDoneFlagWhenBlockedDone() throws Exception {
        Bug bug = new Bug("1");

        TrelloCard card = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(2.0)
                .score(100)
                .blocks(singletonSet(bug))
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(card);

        trigger();

        verify(trelloActions).assignLabelToCard(card, "done?");
    }

    @Test
    public void testNoIsDoneFlagWhenInDocumentation() throws Exception {
        Bug bug = new Bug("1");

        TrelloCard card = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status("documentation")
                .pos(2.0)
                .score(100)
                .blocks(singletonSet(bug))
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(card);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card, "done?");
    }

    @Test
    public void testNoIsDoneFlagWhenNoBlockedBugs() throws Exception {
        TrelloCard card = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(2.0)
                .score(100)
                .blocks(new HashSet<>())
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(card);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card, "done?");
    }

    @Test
    public void testNoIsDoneFlagWhenNotAllBlockedDone() throws Exception {
        final Bug bugId2 = new Bug("1");

        BugzillaBug bug1 = newBug(1, BugzillaStatus.ASSIGNED)
                .build();

        TrelloCard card = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(2.0)
                .score(100)
                .blocks(new HashSet<>(Arrays.asList(bugId2, bug1.getBug())))
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(bug1);
        factService.addFact(card);

        trigger();

        verify(trelloActions, never()).assignLabelToCard(card, "done?");
    }

    @Test
    public void testIsDoneFlagWhenBlockedModified() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.MODIFIED)
                .build();

        TrelloCard card = TrelloCard.builder()
                .id("b")
                .closed(false)
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(2.0)
                .score(100)
                .blocks(singletonSet(bug1.getBug()))
                .assignedTo(new HashSet<>())
                .build();

        factService.addFact(bug1);
        factService.addFact(card);

        trigger();

        verify(trelloActions).assignLabelToCard(card, "done?");
    }
    
    public void testBlockingBugPmScore() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.MODIFIED)
                .pmScore(100)
                .build();
        BugzillaBug bug2 = newBug(2, BugzillaStatus.MODIFIED)
                .blocks(singletonSet(bug1.getBug()))
                .pmScore(10)
                .build();

        factService.addOrUpdateFact(bug1);
        factService.addOrUpdateFact(bug2);
        trigger();

        assertThat(bug1.getPmScore())
                .isEqualTo(bug2.getPmScore());
    }

    public void testBlockingBugPmPriority() throws Exception {
        BugzillaBug bug1 = newBug(1, BugzillaStatus.MODIFIED)
                .pmPriority(10)
                .build();
        BugzillaBug bug2 = newBug(2, BugzillaStatus.MODIFIED)
                .blocks(singletonSet(bug1.getBug()))
                .pmPriority(100)
                .build();

        factService.addOrUpdateFact(bug1);
        factService.addOrUpdateFact(bug2);
        trigger();

        assertThat(bug1.getPmPriority())
                .isEqualTo(bug2.getPmPriority());
    }

    public <T> Set<T> singletonSet(T element) {
        return new HashSet<>(Collections.singletonList(element));
    }
}
