package org.marsik.bugautomation.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.kie.api.cdi.KSession;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;

@Singleton
public class RuleGlobalsService {
    @Inject
    @KSession("bug-rules")
    KieSession kSession;

    @Inject
    InternalActions internalActions;

    @Inject
    BugzillaActions bugzillaActions;

    @Inject
    TrelloActions trelloActions;

    @Inject
    ConfigurationService configurationService;

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
            dtoRow.put("bug", row.get("bug"));
            dtoRow.put("card", row.get("card"));
            dtos.add(dtoRow);
        }
        return dtos;
    }
}
