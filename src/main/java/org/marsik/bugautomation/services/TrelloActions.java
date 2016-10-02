package org.marsik.bugautomation.services;

import org.marsik.bugautomation.facts.BugzillaBug;
import org.marsik.bugautomation.facts.TrelloBoard;
import org.marsik.bugautomation.facts.TrelloCard;
import org.marsik.bugautomation.facts.TrelloLabel;
import org.marsik.bugautomation.facts.User;

public interface TrelloActions {
    void createCard(TrelloBoard kiBoard, String listName, BugzillaBug bug, User assignTo);

    void switchCards(TrelloCard one, TrelloCard two);

    void moveCard(TrelloCard kiCard, TrelloBoard kiBoard, String listName);

    void assignCard(TrelloCard card, User user);

    void assignLabelToCard(TrelloCard kiCard, String labelName);

    void removeLabelFromCard(TrelloCard kiCard, TrelloLabel kiLabel);
}
