package org.marsik.bugautomation.jobs;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

import org.marsik.bugautomation.facts.Assignment;
import org.marsik.bugautomation.facts.Bug;
import org.marsik.bugautomation.facts.TrelloBoard;
import org.marsik.bugautomation.facts.TrelloCard;
import org.marsik.bugautomation.facts.TrelloLabel;
import org.marsik.bugautomation.facts.User;
import org.marsik.bugautomation.services.BugMatchingService;
import org.marsik.bugautomation.services.ConfigurationService;
import org.marsik.bugautomation.services.FactService;
import org.marsik.bugautomation.services.UserMatchingService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trello4j.Trello;
import org.trello4j.TrelloImpl;
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

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        final Optional<String> trelloAppKey = configurationService.get(ConfigurationService.TRELLO_APP_KEY);
        final Optional<String> trelloToken = configurationService.get(ConfigurationService.TRELLO_TOKEN);

        if (!trelloAppKey.isPresent() || !trelloToken.isPresent()) {
            logger.warn("Trello not configured, skipping refresh.");
            return;
        }

        Map<String, User> users = new HashMap<>();

        Trello trello = new TrelloImpl(trelloAppKey.get(), trelloToken.get());
        List<String> boards = Arrays.asList(configurationService.get(ConfigurationService.TRELLO_BOARDS)
                .orElse("").split(" *, *"));

        // Load users
        boards.stream()
                .map(trello::getMembersByBoard)
                .flatMap(Collection::stream)
                .forEach(user -> {
                    userMatchingService.getByUsername(user.getUsername()).ifPresent(u -> {
                        logger.info("Found user {}", user.getFullName());
                        users.put(user.getId(), u);
                    });
                });


        // Process boards
        for (String boardId: boards) {
            Board board = trello.getBoard(boardId);
            final TrelloBoard trelloBoard = TrelloBoard.builder()
                    .name(board.getName())
                    .id(board.getId())
                    .build();
            logger.info("Found board {}", trelloBoard.getName());
            factService.addOrUpdateFact(trelloBoard);

            Map<String, String> idListToStatus = new HashMap<>();

            // Process cards
            for (Card card: trello.getCardsByBoard(boardId)) {
                String status = idListToStatus.get(card.getIdList());
                if (status == null) {
                    org.trello4j.model.List list = trello.getList(card.getIdList());
                    idListToStatus.put(list.getId(), list.getName());
                    status = list.getName();
                }

                TrelloCard trelloCard = TrelloCard.builder()
                        .id(card.getId())
                        .title(card.getName())
                        .description(card.getDesc())
                        .board(trelloBoard)
                        .pos(card.getPos())
                        .status(status.toLowerCase().replace(" ", ""))
                        .build();

                logger.info("Found card {} at {}#{}", trelloCard.getTitle(), trelloCard.getStatus(), trelloCard.getPos());

                // Add label facts
                card.getLabels().stream()
                        .map(l -> new TrelloLabel(l.getColor().toLowerCase(), l.getName().toLowerCase(), trelloCard))
                        .forEach(factService::addOrUpdateFact);

                // Add assignment facts
                card.getIdMembers().stream()
                        .map(users::get)
                        .filter(u -> u != null)
                        .distinct()
                        .map(u -> {
                            logger.info("Card {} assigned to {}", trelloCard.getTitle(), u.getName());
                            return Assignment.builder()
                                    .user(u)
                                    .target(trelloCard)
                                    .build();
                        })
                        .forEach(factService::addOrUpdateFact);

                // Find bugs
                Optional<Bug> bug = bugMatchingService.identifyBug(card.getName());
                if (!bug.isPresent()) {
                    bug = bugMatchingService.identifyBug(card.getDesc());
                }

                if (bug.isPresent()) {
                    logger.info("Card {} is tied to virtual bug {} (rhbz#{})", trelloCard.getTitle(), bug.get().getId(), bugMatchingService.getBzBug(bug.get()));
                }
                factService.addOrUpdateFact(trelloCard);
            }
        }
    }
}
