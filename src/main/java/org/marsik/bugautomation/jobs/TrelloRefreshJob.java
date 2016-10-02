package org.marsik.bugautomation.jobs;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.marsik.bugautomation.facts.Bug;
import org.marsik.bugautomation.facts.TrelloBoard;
import org.marsik.bugautomation.facts.TrelloCard;
import org.marsik.bugautomation.facts.TrelloLabel;
import org.marsik.bugautomation.facts.User;
import org.marsik.bugautomation.services.BugMatchingService;
import org.marsik.bugautomation.services.ConfigurationService;
import org.marsik.bugautomation.services.FactService;
import org.marsik.bugautomation.services.TrelloActionsImpl;
import org.marsik.bugautomation.services.UserMatchingService;
import org.marsik.bugautomation.trello.Board;
import org.marsik.bugautomation.trello.Card;
import org.marsik.bugautomation.trello.TrelloClient;
import org.marsik.bugautomation.trello.TrelloClientBuilder;
import org.marsik.bugautomation.trello.TrelloList;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisallowConcurrentExecution
public class TrelloRefreshJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(TrelloRefreshJob.class);
    private static final AtomicBoolean finished = new AtomicBoolean(false);

    @Inject
    FactService factService;

    @Inject
    ConfigurationService configurationService;

    @Inject
    UserMatchingService userMatchingService;

    @Inject
    BugMatchingService bugMatchingService;

    @Inject
    TrelloActionsImpl trelloActions;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        TrelloClientBuilder builder = trelloActions.getTrello();
        if (builder == null) {
            logger.warn("Trello not configured, can't create card.");
            return;
        }

        TrelloClient trello = builder.build();

        Map<String, User> users = new HashMap<>();

        List<String> boards = Arrays.asList(configurationService.get(ConfigurationService.TRELLO_BOARDS)
                .orElse("").split(" *, *"));

        // Process boards
        for (String boardId: boards) {
            Board trBoard = trello.getBoardWithData(boardId, "open", "open", "all");
            final TrelloBoard kiBoard = TrelloBoard.builder()
                    .name(trBoard.getName())
                    .id(trBoard.getId())
                    .build();
            logger.debug("Found board {}", kiBoard.getName());
            factService.addOrUpdateFact(kiBoard);

            Map<String, String> idListToStatus = new HashMap<>();
            Map<String, TrelloList> idToListMap = new HashMap<>();

            // Load lists
            for (TrelloList list: trBoard.getLists()) {
                idToListMap.put(list.getId(), list);
            }

            // Load users
            trBoard.getMembers().forEach(user -> {
                        userMatchingService.getByTrello(user.getId()).ifPresent(u -> {
                            logger.debug("Found user {} ({})", user.getId(), user.getFullName());
                            users.put(user.getId(), u);
                        });
                    });

            // Process cards
            for (Card trCard: trBoard.getCards()) {
                String status = idListToStatus.get(trCard.getIdList());
                if (status == null) {
                    TrelloList list = idToListMap.get(trCard.getIdList());
                    idListToStatus.put(list.getId(), list.getName());
                    status = list.getName();
                }

                TrelloCard kiCard = TrelloCard.builder()
                        .id(trCard.getId())
                        .title(trCard.getName())
                        .description(trCard.getDesc())
                        .board(kiBoard)
                        .pos(trCard.getPos())
                        .status(status.toLowerCase().replace(" ", ""))
                        .assignedTo(new ArrayList<>())
                        .labels(new ArrayList<>())
                        .build();

                logger.debug("Found card {} at {}#{}", kiCard.getTitle(), kiCard.getStatus(), kiCard.getPos());

                // Add label facts
                trCard.getLabels().stream()
                        .map(l -> new TrelloLabel(kiBoard, l.getId(), l.getColor().toLowerCase(), l.getName().toLowerCase()))
                        .forEach(kiCard.getLabels()::add);

                // Add assignment facts
                trCard.getIdMembers().stream()
                        .map(users::get)
                        .filter(u -> u != null)
                        .distinct()
                        .forEach(u -> {
                            logger.debug("Card {} assigned to {}", kiCard.getTitle(), u.getName());
                            kiCard.getAssignedTo().add(u);
                        });

                // Find bugs
                Optional<Bug> bug = bugMatchingService.identifyBug(trCard.getName());
                if (!bug.isPresent()) {
                    bug = bugMatchingService.identifyBug(trCard.getDesc());
                }

                if (bug.isPresent()) {
                    logger.debug("Card {} is tied to virtual bug {}", kiCard.getTitle(), bug.get().getId());
                    kiCard.setBug(bug.get());
                }

                factService.addOrUpdateFact(kiCard);
            }
        }

        finished.set(true);
    }

    public static AtomicBoolean getFinished() {
        return finished;
    }
}
