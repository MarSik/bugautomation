package org.marsik.bugautomation.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.marsik.bugautomation.facts.BugzillaBug;
import org.marsik.bugautomation.facts.GithubIssue;
import org.marsik.bugautomation.facts.TrelloCard;

@ApplicationScoped
public class RuleGlobalsService {
    @Inject
    InternalActions internalActions;

    @Inject
    BugzillaActions bugzillaActions;

    @Inject
    TrelloActions trelloActions;

    @Inject
    ConfigurationService configurationService;

    @Inject
    KieSession kSession;

    @PostConstruct
    public void init() {
        kSession.setGlobal("bugzilla", bugzillaActions);
        kSession.setGlobal("trello", trelloActions);
        kSession.setGlobal("config", configurationService);
        kSession.setGlobal("internal", internalActions);
    }

    public List<Map<String, Object>> getBugInfo(String bugId) {
        QueryResults results = kSession.getQueryResults("getBugInfo", bugId);
        List<Map<String, Object>> dtos = new ArrayList<>();
        for (QueryResultsRow row: results) {
            Map<String, Object> dtoRow = new HashMap<>();
            dtoRow.put("bug", row.get("bz"));
            dtoRow.put("card", row.get("card"));
            dtoRow.put("order", row.get("order"));
            dtos.add(dtoRow);
        }
        return dtos;
    }

    @SuppressWarnings("unchecked")
    public Collection<BugzillaBug> getBugzillaBugs() {
        return (Collection<BugzillaBug>) kSession.getObjects(o -> o instanceof BugzillaBug);
    }

    @SuppressWarnings("unchecked")
    public Collection<TrelloCard> getTrelloCards() {
        return (Collection<TrelloCard>) kSession.getObjects(o -> o instanceof TrelloCard);
    }

    @SuppressWarnings("unchecked")
    public Collection<GithubIssue> getGithubIssues() {
        return (Collection<GithubIssue>) kSession.getObjects(o -> o instanceof GithubIssue);
    }
}
