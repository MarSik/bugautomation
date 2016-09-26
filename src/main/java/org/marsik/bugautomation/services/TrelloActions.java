package org.marsik.bugautomation.services;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import org.marsik.bugautomation.facts.BugzillaBug;
import org.marsik.bugautomation.facts.TrelloBoard;
import org.marsik.bugautomation.facts.TrelloCard;
import org.marsik.bugautomation.facts.User;
import org.marsik.bugautomation.trello.Card;
import org.marsik.bugautomation.trello.TrelloClient;
import org.marsik.bugautomation.trello.TrelloClientBuilder;
import org.marsik.bugautomation.trello.TrelloList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TrelloActions {
    private static final Logger logger = LoggerFactory.getLogger(TrelloActions.class);

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

    public void createCard(TrelloBoard kiBoard, String listName, BugzillaBug bug, User assignTo) {
        TrelloClientBuilder builder = getTrello();
        if (builder == null) {
            logger.warn("Trello not configured, can't create card.");
            return;
        }

        TrelloClient trello = builder.build();

        final Optional<String> bugzillaUrl = configurationService.get(ConfigurationService.BUGZILLA_URL);
        String desc = "";
        if (bugzillaUrl.isPresent()) {
            URI uri = URI.create(bugzillaUrl.get()).resolve(bug.getId());
            desc = uri.toString();
        }

        java.util.List<TrelloList> trLists = trello.getListByBoard(kiBoard.getId());
        Optional<TrelloList> trList = trLists.stream().filter(l -> l.getName().equalsIgnoreCase(listName)).findFirst();

        if (!trList.isPresent()) {
            logger.error("Could not find list {}/{} to create a card for bug#{}", kiBoard.getName(), listName, bug.getId());
            return;
        }

        TrelloCard kiCard = TrelloCard.builder()
                .title(bug.getId() + " - " + bug.getTitle())
                .description(desc)
                .bug(bug.getBug())
                .board(kiBoard)
                .assignedTo(Collections.singletonList(assignTo))
                .labels(new ArrayList<>())
                .build();

        final HashMap<String, String> attrMap = new HashMap<>();
        userMatchingService.getTrello(assignTo)
                .ifPresent(trUser -> attrMap.put("idMembers", trUser));

        attrMap.put("name", kiCard.getTitle());
        attrMap.put("desc", kiCard.getDescription());

        Card trCard = trello.createCard(trList.get().getId(), attrMap);
        if (trCard != null) factService.addFact(kiCard);
    }
}
