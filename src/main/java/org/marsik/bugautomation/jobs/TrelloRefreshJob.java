package org.marsik.bugautomation.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

import org.marsik.bugautomation.facts.Bug;
import org.marsik.bugautomation.facts.TrelloBoard;
import org.marsik.bugautomation.facts.TrelloCard;
import org.marsik.bugautomation.facts.TrelloLabel;
import org.marsik.bugautomation.facts.User;
import org.marsik.bugautomation.services.BugMatchingService;
import org.marsik.bugautomation.services.ConfigurationService;
import org.marsik.bugautomation.services.FactService;
import org.marsik.bugautomation.services.TrelloActions;
import org.marsik.bugautomation.services.UserMatchingService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trello4j.Trello;
import org.trello4j.model.Board;
import org.trello4j.model.Card;

@DisallowConcurrentExecution
public class TrelloRefreshJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(TrelloRefreshJob.class);

    @Inject
    FactService factService;

    @Inject
    ConfigurationService configurationService;

    @Inject
    UserMatchingService userMatchingService;

    @Inject
    BugMatchingService bugMatchingService;

    @Inject
    TrelloActions trelloActions;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        Trello trello = trelloActions.getTrello();
        if (trello == null) {
            logger.warn("Trello not configured, skipping refresh.");
            return;
        }

        Map<String, User> users = new HashMap<>();

        List<String> boards = Arrays.asList(configurationService.get(ConfigurationService.TRELLO_BOARDS)
                .orElse("").split(" *, *"));

        // Load users
        boards.stream()
                .map(trello::getMembersByBoard)
                .flatMap(Collection::stream)
                .forEach(user -> {
                    userMatchingService.getByTrello(user.getId()).ifPresent(u -> {
                        logger.info("Found user {} ({})", user.getId(), user.getFullName());
                        users.put(user.getId(), u);
                    });
                });


        // Process boards
        for (String boardId: boards) {
            Board trBoard = trello.getBoard(boardId);
            final TrelloBoard kiBoard = TrelloBoard.builder()
                    .name(trBoard.getName())
                    .id(trBoard.getId())
                    .build();
            logger.info("Found board {}", kiBoard.getName());
            factService.addOrUpdateFact(kiBoard);

            Map<String, String> idListToStatus = new HashMap<>();

            // Process cards
            for (Card trCard: trello.getCardsByBoard(boardId)) {
                String status = idListToStatus.get(trCard.getIdList());
                if (status == null) {
                    org.trello4j.model.List list = trello.getList(trCard.getIdList());
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

                logger.info("Found card {} at {}#{}", kiCard.getTitle(), kiCard.getStatus(), kiCard.getPos());

                // Add label facts
                trCard.getLabels().stream()
                        .map(l -> new TrelloLabel(kiBoard, l.getColor().toLowerCase(), l.getColor(), l.getName().toLowerCase()))
                        .forEach(kiCard.getLabels()::add);

                // Add assignment facts
                trCard.getIdMembers().stream()
                        .map(users::get)
                        .filter(u -> u != null)
                        .distinct()
                        .forEach(u -> {
                            logger.info("Card {} assigned to {}", kiCard.getTitle(), u.getName());
                            kiCard.getAssignedTo().add(u);
                        });

                // Find bugs
                Optional<Bug> bug = bugMatchingService.identifyBug(trCard.getName());
                if (!bug.isPresent()) {
                    bug = bugMatchingService.identifyBug(trCard.getDesc());
                }

                if (bug.isPresent()) {
                    logger.info("Card {} is tied to virtual bug {} (rhbz#{})", kiCard.getTitle(), bug.get().getId(), bugMatchingService.getBzBug(bug.get()));
                    kiCard.setBug(bug.get());
                }

                factService.addOrUpdateFact(kiCard);
            }
        }
    }


}
