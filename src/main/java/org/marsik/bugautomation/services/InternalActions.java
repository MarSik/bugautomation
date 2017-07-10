package org.marsik.bugautomation.services;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Objects;

import org.marsik.bugautomation.facts.TrelloCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class InternalActions {
    private static final Logger logger = LoggerFactory.getLogger(InternalActions.class);

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private FactService factService;

    @Inject
    UserMatchingService userMatchingService;

    public void setScore(TrelloCard kiCard, Integer score) {
        if (Objects.equals(kiCard.getScore(), score)) return;

        kiCard.setScore(score);
        factService.addOrUpdateFact(kiCard);
    }
}
