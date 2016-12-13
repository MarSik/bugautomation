package org.marsik.bugautomation.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.kie.api.cdi.KSession;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.ObjectFilter;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.marsik.bugautomation.facts.BugzillaBug;

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
}
