package org.marsik.bugautomation.jobs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import javax.inject.Inject;

import org.marsik.bugautomation.facts.Assignment;
import org.marsik.bugautomation.facts.Bug;
import org.marsik.bugautomation.facts.BugzillaBug;
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

import b4j.core.DefaultIssue;
import b4j.core.SearchData;
import b4j.core.session.BugzillaHttpSession;
import b4j.core.DefaultSearchData;
import b4j.core.Issue;
import rs.baselib.security.AuthorizationCallback;
import rs.baselib.security.SimpleAuthorizationCallback;

@DisallowConcurrentExecution
public class BugzillaRefreshJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(BugzillaRefreshJob.class);

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
        final Optional<String> bugzillaUrl = configurationService.get(ConfigurationService.BUGZILLA_URL);
        final Optional<String> bugzillaUsername = configurationService.get(ConfigurationService.BUGZILLA_USERNAME);
        final Optional<String> bugzillaPassword = configurationService.get(ConfigurationService.BUGZILLA_PASSWORD);
        final Optional<String> bugzillaOwners = configurationService.get(ConfigurationService.BUGZILLA_OWNERS);
        final Optional<String> bugzillaTeams = configurationService.get(ConfigurationService.BUGZILLA_TEAMS);

        if (!bugzillaUrl.isPresent()
                || !bugzillaUsername.isPresent()
                || !bugzillaPassword.isPresent()) {
            logger.warn("Bugzilla not configured");
            return;
        }

        BugzillaHttpSession session = new BugzillaHttpSession();
        try {
            session.setBaseUrl(new URL(bugzillaUrl.get()));
        } catch (MalformedURLException e) {
            logger.error("Bugzilla url incorrect", e);
            return;
        }

        session.setBugzillaBugClass(DefaultIssue.class);

        AuthorizationCallback authCallback = new SimpleAuthorizationCallback(bugzillaUsername.get(), bugzillaPassword.get());
        session.getHttpSessionParams().setAuthorizationCallback(authCallback);

        if (session.open()) {
            // Search bugs by users
            DefaultSearchData searchData = new DefaultSearchData();
            if (bugzillaOwners.isPresent() && !bugzillaOwners.get().trim().isEmpty()) {
                for (String owner : splitNames(bugzillaOwners)) {
                    searchData.add("assigned_to", owner);
                }
                populateSearchData(searchData);
                searchAndProcess(session, searchData);
            }

            // Search bugs by teams
            if (bugzillaTeams.isPresent() && !bugzillaTeams.get().trim().isEmpty()) {
                searchData = new DefaultSearchData();
                for (String team : splitNames(bugzillaTeams)) {
                    searchData.add("team", team);
                }
                populateSearchData(searchData);
                searchAndProcess(session, searchData);
            }

            // Close the session
            session.close();
        }

    }

    private String[] splitNames(Optional<String> commaSeparatedNames) {
        return commaSeparatedNames.orElse("").split(" *, *");
    }

    private void populateSearchData(SearchData searchData) {
        searchData.add("bug_status", "NEW");
        searchData.add("bug_status", "ASSIGNED");
        searchData.add("bug_status", "POST");
        searchData.add("bug_status", "MODIFIED");
    }

    private void searchAndProcess(BugzillaHttpSession session, SearchData searchData) {
        Iterable<Issue> i = session.searchBugs(searchData, i1 -> logger.info("Loading BZ results.. {}", i1));
        for (Issue issue : i) {
            System.out.println("Bug found: " + issue.getId() + " - " + issue.getStatus() + " - " + issue.getAssignee().getRealName() + " - " + issue.getSummary());

            BugzillaBug bugzillaBug = BugzillaBug.builder()
                    .title(issue.getSummary())
                    .description(issue.getDescription())
                    .status(issue.getStatus().getName().toLowerCase())
                    .bug(bugMatchingService.getBugByBzId(issue.getId()))
                    .build();

            factService.addOrUpdateFact(bugzillaBug);

            userMatchingService.getByEmail(issue.getAssignee().getId()).ifPresent(
                    u -> {
                        logger.info("Bug {} assigned to {}", issue.getId(), u.getName());
                        factService.addOrUpdateFact(Assignment.builder()
                                .user(u)
                                .target(bugzillaBug)
                                .build());
                    }
            );
        }
    }
}
