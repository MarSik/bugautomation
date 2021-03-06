package org.marsik.bugautomation.services;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import org.marsik.bugautomation.facts.BugzillaBug;
import org.marsik.bugautomation.facts.GenericIssue;
import org.marsik.bugautomation.facts.GithubIssue;
import org.marsik.bugautomation.facts.TrelloBoard;
import org.marsik.bugautomation.facts.TrelloCard;
import org.marsik.bugautomation.facts.TrelloLabel;
import org.marsik.bugautomation.facts.User;
import org.marsik.bugautomation.trello.Card;
import org.marsik.bugautomation.trello.Label;
import org.marsik.bugautomation.trello.TrelloClient;
import org.marsik.bugautomation.trello.TrelloClientBuilder;
import org.marsik.bugautomation.trello.TrelloList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TrelloActionsImpl implements TrelloActions {
    private static final Logger logger = LoggerFactory.getLogger(TrelloActionsImpl.class);

    @Inject
    ConfigurationService configurationService;

    @Inject
    FactService factService;

    @Inject
    UserMatchingService userMatchingService;

    public TrelloClientBuilder getTrello() {
        final Optional<String> trelloAppKey = configurationService.get(ConfigurationService.TRELLO_APP_KEY);
        final Optional<String> trelloToken = configurationService.get(ConfigurationService.TRELLO_TOKEN);

        if (!trelloAppKey.isPresent() || !trelloToken.isPresent()) {
            return null;
        }

        TrelloClientBuilder builder = new TrelloClientBuilder(trelloAppKey.get(), trelloToken.get());
        return builder;
    }

    @Override
    public void createCard(TrelloBoard kiBoard, String listName, GenericIssue bug, Collection<User> assignTo) {
        TrelloClientBuilder builder = getTrello();
        if (builder == null) {
            logger.warn("Trello not configured, can't create card.");
            return;
        }

        TrelloClient trello = builder.build();

        String desc = bug.getUrl(configurationService).orElse("");

        final Optional<TrelloList> trList;
        try {
            java.util.List<TrelloList> trLists = trello.getListByBoard(kiBoard.getId());
            trList = trLists.stream().filter(l -> l.getName().equalsIgnoreCase(listName)).findFirst();
        } catch (ClientErrorException ex) {
            logger.error("Could not retrieve list to create a card for.", ex);
            return;
        }

        if (!trList.isPresent()) {
            logger.error("Could not find list {}/{} to create a card for ", kiBoard.getName(), listName, bug.getTitleId());
            return;
        }

        TrelloCard kiCard = TrelloCard.builder()
                .title(bug.getTitleId() + " - " + bug.getTitle())
                .description(desc)
                .bug(bug.getBug())
                .board(kiBoard)
                .assignedTo(new HashSet<>(assignTo))
                .labels(new HashSet<>())
                .status(trList.get().getName().replace(" ", "").toLowerCase())
                .build();

        final HashMap<String, Object> attrMap = new HashMap<>();
        String users = assignTo.stream()
                .filter(kiBoard.getMembers()::contains)
                .map(userMatchingService::getTrello)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining(","));

        attrMap.put("idMembers", users);
        attrMap.put("name", kiCard.getTitle());

        String description = kiCard.getDescription();

        if (kiCard.getBug() != null) {
            description += "\n\n{{ id:" + kiCard.getBug().getId() + " }}";
        }

        attrMap.put("desc", description);

        try {
            Card trCard = trello.createCard(trList.get().getId(), attrMap);
            if (trCard != null) {
                kiCard.setId(trCard.getId());
                kiCard.setPos(trCard.getPos());
                factService.addFact(kiCard);
            }
        } catch (ClientErrorException ex) {
            logger.error("Could not create card.", ex);
        }
    }

    @Override
    public void switchCards(TrelloCard one, TrelloCard two) {
        TrelloClientBuilder builder = getTrello();
        if (builder == null) {
            logger.warn("Trello not configured, can't create card.");
            return;
        }

        TrelloClient trello = builder.build();

        double pos0 = one.getPos();
        one.setPos(two.getPos());
        two.setPos(pos0);

        logger.info("Switching position of {} ({}) and {} ({})", one.getId(), one.getBug(), two.getId(), two.getBug());
        try {
            trello.updateCard(one.getId(), Collections.singletonMap("pos", one.getPos()));
            factService.addOrUpdateFact(one);
        } catch (NotFoundException ex) {
            logger.warn("Card {} not found, removing from facts");
            factService.removeFact(one);
        }

        try {
            trello.updateCard(two.getId(), Collections.singletonMap("pos", two.getPos()));
            factService.addOrUpdateFact(two);
        } catch (NotFoundException ex) {
            logger.warn("Card {} not found, removing from facts");
            factService.removeFact(two);
        }
    }

    @Override
    public void moveCard(TrelloCard kiCard, TrelloBoard kiBoard, String listName) {
        TrelloClientBuilder builder = getTrello();
        if (builder == null) {
            logger.warn("Trello not configured, can't create card.");
            return;
        }

        TrelloClient trello = builder.build();

        java.util.List<TrelloList> trLists = trello.getListByBoard(kiBoard.getId());
        Optional<TrelloList> trList = trLists.stream().filter(l -> l.getName().equalsIgnoreCase(listName)).findFirst();

        if (!trList.isPresent()) {
            logger.error("Could not find list {}/{} to move a card {}", kiBoard.getName(), listName, kiCard);
            return;
        }

        try {
            logger.info("Moving a card {} from {} to {}",
                    kiCard.getTitle(),
                    kiCard.getStatus(),
                    trList.get().getName());
            trello.moveCard(kiCard.getId(), trList.get().getId());

            if (kiCard.isClosed()) {
                logger.info("Unarchiving card {}", kiCard.getTitle());
                trello.archiveCard(kiCard.getId(), Collections.singletonMap("value", false));
                kiCard.setClosed(false);
            }

            kiCard.setBoard(kiBoard);
            kiCard.setStatus(trList.get().getName().replace(" ", "").toLowerCase());
            factService.addOrUpdateFact(kiCard);
        }  catch (NotFoundException ex) {
            logger.warn("Card {} not found, removing from facts");
            factService.removeFact(kiCard);
        }
    }

    @Override
    public void assignCard(TrelloCard card, User user) {
        TrelloClientBuilder builder = getTrello();
        if (builder == null) {
            logger.warn("Trello not configured, can't create card.");
            return;
        }

        TrelloClient trello = builder.build();
        userMatchingService.getTrello(user).ifPresent(userId -> {
            logger.info("Assigning {} to {}", card, user);
            trello.assignCardToUser(card.getId(), userId);
            card.getAssignedTo().add(user);
            factService.addOrUpdateFact(card);
        });
    }

    @Override
    public void assignLabelToCard(TrelloCard kiCard, String labelName) {
        TrelloClientBuilder builder = getTrello();
        if (builder == null) {
            logger.warn("Trello not configured, can't assign label.");
            return;
        }

        logger.info("Assigning {} to {}", labelName, kiCard);

        TrelloClient trello = builder.build();

        Optional<Label> trLabel = trello.getBoardLabels(kiCard.getBoard().getId()).stream()
                .filter(l -> l.getName().equalsIgnoreCase(labelName))
                .findFirst();

        if (!trLabel.isPresent()) {
            logger.error("Could not find label {} to add to a card {}", labelName, kiCard);
            return;
        }

        trello.addLabelToCard(kiCard.getId(), trLabel.get().getId());
        kiCard.getLabels().add(TrelloLabel.builder()
                .id(trLabel.get().getId())
                .name(trLabel.get().getName().toLowerCase())
                .color(trLabel.get().getColor().toLowerCase())
                .build());
        factService.addOrUpdateFact(kiCard);
    }

    @Override
    public void removeLabelFromCard(TrelloCard kiCard, TrelloLabel kiLabel) {
        TrelloClientBuilder builder = getTrello();
        if (builder == null) {
            logger.warn("Trello not configured, can't remove label.");
            return;
        }

        logger.info("Removing {} from {}", kiLabel, kiCard);

        TrelloClient trello = builder.build();
        trello.removeLabelFromCard(kiCard.getId(), kiLabel.getId());
        kiCard.getLabels().remove(kiLabel);
        factService.addOrUpdateFact(kiCard);
    }
}
