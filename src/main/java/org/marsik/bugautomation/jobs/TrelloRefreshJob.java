package org.marsik.bugautomation.jobs;

import javax.inject.Inject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.marsik.bugautomation.facts.Bug;
import org.marsik.bugautomation.facts.TrelloBoard;
import org.marsik.bugautomation.facts.TrelloCard;
import org.marsik.bugautomation.facts.TrelloLabel;
import org.marsik.bugautomation.facts.User;
import org.marsik.bugautomation.services.BugMatchingService;
import org.marsik.bugautomation.services.ConfigurationService;
import org.marsik.bugautomation.services.FactService;
import org.marsik.bugautomation.services.RuleGlobalsService;
import org.marsik.bugautomation.services.StatsService;
import org.marsik.bugautomation.services.TrelloActionsImpl;
import org.marsik.bugautomation.services.UserMatchingService;
import org.marsik.bugautomation.stats.SingleStat;
import org.marsik.bugautomation.stats.Stats;
import org.marsik.bugautomation.trello.Board;
import org.marsik.bugautomation.trello.Card;
import org.marsik.bugautomation.trello.TrelloClient;
import org.marsik.bugautomation.trello.TrelloClientBuilder;
import org.marsik.bugautomation.trello.TrelloList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrelloRefreshJob implements Runnable {
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

    @Inject
    RuleGlobalsService ruleGlobalsService;

    @Inject StatsService statsService;

    private static final Pattern CUSTOM_FIELDS_GROUP_RE = Pattern.compile("\\{\\{ *(([a-zA-Z0-9]+[:=][a-zA-Z0-9@.:/_=?-]*) *)* *\\}\\}");
    private static final Pattern CUSTOM_FIELDS_RE = Pattern.compile("([a-zA-Z0-9]+)[=:]([a-zA-Z0-9@.:/_=?-]*)");

    @Override
    public void run() {

        TrelloClientBuilder builder = trelloActions.getTrello();
        if (builder == null) {
            logger.warn("Trello not configured, can't create card.");
            return;
        }

        long startTime = System.nanoTime();

        TrelloClient trello = builder.build();

        Map<String, User> users = new HashMap<>();

        List<String> boards = configurationService.getMonitoredBoards();

        // Process boards
        Set<String> visitedCards = new HashSet<>();

        for (String boardId: boards) {
            logger.info("Refreshing trello board {}", boardId);
            Board trBoard = trello.getBoardWithData(boardId, "all", "all", "all");
            final TrelloBoard kiBoard = TrelloBoard.builder()
                    .name(trBoard.getName())
                    .id(trBoard.getId())
                    .members(new HashSet<>())
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
                            kiBoard.getMembers().add(u);
                        });
                    });

            // Process cards
            for (Card trCard: trBoard.getCards()) {
                visitedCards.add(trCard.getId());
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
                        .assignedTo(new HashSet<>())
                        .labels(new HashSet<>())
                        .fields(new HashMap<>())
                        .blocks(new HashSet<>())
                        .closed(Optional.ofNullable(trCard.getClosed()).orElse(false))
                        .build();

                logger.debug("Found card {} at {}#{}", kiCard.getTitle(), kiCard.getStatus(), kiCard.getPos());

                // Process custom flags from description
                Map<String,String> fields = getCustomFields(trCard.getDesc());
                kiCard.getFields().putAll(fields);

                // Ignore when ignore flag is present!
                if (fields.containsKey("ignore")) {
                    continue;
                }

                if (trCard.getDue() != null) {
                    kiCard.setDueDate(Instant.parse(trCard.getDue()));
                }

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
                    // Description lookup has to ignore the field sections
                    bug = bugMatchingService.identifyBug(kiCard.getCleanDesc());
                }

                if (bug.isPresent()) {
                    logger.debug("Card {} is tied to virtual bug {}", kiCard.getTitle(), bug.get().getId());
                    kiCard.setBug(bug.get());
                }

                // Use score if provided
                if (fields.containsKey("score")) {
                    try {
                        kiCard.setScore(Integer.valueOf(fields.get("score")));
                    } catch (NumberFormatException ex) {
                        logger.warn("Card {} contains invalid score value {}", kiCard, fields.get("score"));
                    }
                }

                // Add all blocking bugs
                if (fields.containsKey("blocks")) {
                    String[] blocksList = fields.get("blocks").split(",");
                    for (String blocks: blocksList) {
                        kiCard.getBlocks().add(new Bug(blocks));
                    }
                }

                if (fields.containsKey("bug")) {
                    String[] blocksList = fields.get("bug").split(",");
                    for (String blocks: blocksList) {
                        bug = bugMatchingService.identifyBug(blocks);
                        if (bug.isPresent()) {
                            kiCard.getBlocks().add(bug.get());
                        } else {
                            logger.warn("Card {} contains invalid blocking bug id {}", kiCard, blocks);
                        }
                    }
                }

                // Use target milestone if provided
                if (fields.containsKey("target")) {
                    kiCard.setTargetMilestone(configurationService.resolveRelease(fields.get("target")));
                }

                if (fields.containsKey("targetMilestone")) {
                    kiCard.setTargetMilestone(configurationService.resolveRelease(fields.get("targetMilestone")));
                }

                if (fields.containsKey("targetmilestone")) {
                    kiCard.setTargetMilestone(configurationService.resolveRelease(fields.get("targetmilestone")));
                }

                if (fields.containsKey("id")) {
                    kiCard.setBug(new Bug(fields.get("id")));
                    factService.addOrUpdateFact(kiCard.getBug());
                }

                factService.addOrUpdateFact(kiCard);
            }
        }

        // Forget about removed cards
        ruleGlobalsService.getTrelloCards().stream()
                .filter(c -> !visitedCards.contains(c.getId()))
                .peek(c -> logger.info("Forgetting about card: {} ({})", c.getTitle(), c.getId()))
                .forEach(factService::removeFact);

        long elapsedTime = System.nanoTime() - startTime;

        logger.info("Trello refresh finished ({} ms)", (float)elapsedTime / 1000000);

        final Stats stats = new Stats();
        stats.add(SingleStat.TRELLO_REFRESH_TIME).value(elapsedTime);
        statsService.merge(stats);

        finished.set(true);
    }

    public static AtomicBoolean getFinished() {
        return finished;
    }

    public static Map<String, String> getCustomFields(String text) {
        Map<String, String> values = new HashMap<>();
        Matcher matcher = CUSTOM_FIELDS_GROUP_RE.matcher(text);
        while (matcher.find()) {
            Matcher fieldsMatcher = CUSTOM_FIELDS_RE.matcher(matcher.group(0));
            while (fieldsMatcher.find()) {
                values.put(fieldsMatcher.group(1), fieldsMatcher.group(2));
            }
        }

        return values;
    }
}
