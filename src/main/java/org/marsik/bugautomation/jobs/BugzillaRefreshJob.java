package org.marsik.bugautomation.jobs;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.marsik.bugautomation.bugzilla.AuthorizationCallback;
import org.marsik.bugautomation.bugzilla.BugProxy;
import org.marsik.bugautomation.bugzilla.BugzillaClient;
import org.marsik.bugautomation.facts.BugzillaBug;
import org.marsik.bugautomation.facts.BugzillaPriorityLevel;
import org.marsik.bugautomation.facts.BugzillaStatus;
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

@DisallowConcurrentExecution
public class BugzillaRefreshJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(BugzillaRefreshJob.class);
    private static final AtomicBoolean finished = new AtomicBoolean(false);

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

        BugzillaClient session;
        try {
            session = new BugzillaClient(bugzillaUrl.get());
        } catch (MalformedURLException e) {
            logger.error("Bugzilla url incorrect", e);
            return;
        }

        //session.setBugzillaBugClass(DefaultIssue.class);

        AuthorizationCallback authCallback = new AuthorizationCallback(bugzillaUsername.get(), bugzillaPassword.get());
        session.setAuthorizationCallback(authCallback);

        Map<String, BugzillaBug> retrievedBugs = new HashMap<>();

        if (session.open()) {
            // Search bugs by users
            Multimap<String, Object> searchData = ArrayListMultimap.create();
            if (bugzillaOwners.isPresent() && !bugzillaOwners.get().trim().isEmpty()) {
                for (String owner : splitNames(bugzillaOwners)) {
                    searchData.put("assigned_to", owner);
                }
                populateSearchData(searchData);
                Map<String, BugzillaBug> bugs = searchAndProcess(session, searchData);
                retrievedBugs.putAll(bugs);
            }

            // Search bugs by teams
            if (bugzillaTeams.isPresent() && !bugzillaTeams.get().trim().isEmpty()) {
                searchData = ArrayListMultimap.create();
                for (String team : splitNames(bugzillaTeams)) {
                    searchData.put("cf_ovirt_team", team);
                }
                populateSearchData(searchData);
                Map<String, BugzillaBug> bugs = searchAndProcess(session, searchData);
                retrievedBugs.putAll(bugs);
            }

            // Load flags
            for (BugProxy bzExtra: session.getExtra(retrievedBugs.keySet())) {
                bzExtra.loadFlags(bzExtra);
                retrievedBugs.get(bzExtra.getId()).setFlags(bzExtra.getFlags());
            }

            // Update fact database
            retrievedBugs.values().stream().forEach(factService::addOrUpdateFact);

            // Forget about bugs that were assigned out of scope
            Set<String> bugsToRemove = new HashSet<>(bugMatchingService.getKnownBugs());
            bugsToRemove.removeAll(retrievedBugs.keySet());

            bugsToRemove.stream()
                    .map(bugMatchingService::getBugByBzId)
                    .forEach(factService::removeFact);

            // Close the session
            session.close();

            finished.set(true);
        }

    }

    private String[] splitNames(Optional<String> commaSeparatedNames) {
        return commaSeparatedNames.orElse("").split(" *, *");
    }

    private void populateSearchData(Multimap<String, Object> searchData) {
        searchData.put("bug_status", "NEW");
        searchData.put("bug_status", "ASSIGNED");
        searchData.put("bug_status", "POST");
        searchData.put("bug_status", "MODIFIED");
        searchData.put("bug_status", "ON_QA");
    }

    private Map<String, BugzillaBug> searchAndProcess(BugzillaClient session, Multimap<String, Object> searchData) {
        logger.info("Refreshing bugzilla bugs");
        Map<String, BugzillaBug> kiBugs = new HashMap<>();
        Iterable<BugProxy> i = session.searchBugs(searchData);
        for (BugProxy issue : i) {
             BugzillaBug.BugzillaBugBuilder bugzillaBugBuilder = BugzillaBug.builder()
                     .id(issue.getId())
                     .community(issue.getCommunity().toLowerCase().trim().replace(" ", ""))
                     .title(issue.getSummary())
                     .description(issue.getDescription())
                     .status(BugzillaStatus.valueOf(issue.getStatus().toUpperCase()))
                     .bug(bugMatchingService.getBugByBzId(issue.getId()))
                     .severity(BugzillaPriorityLevel.valueOf(issue.getSeverity().toUpperCase()))
                     .priority(BugzillaPriorityLevel.valueOf(issue.getPriority().toUpperCase()))
                     .verified(issue.getVerified().stream().map(String::toLowerCase).collect(Collectors.toSet()))
                     .keywords(issue.getKeywords().stream().map(String::toLowerCase).collect(Collectors.toSet()))
                     .blocks(issue.getBlocks().stream().map(bugMatchingService::getBugByBzId).collect(Collectors.toSet()));

            if (issue.getTargetMilestone() != null) {
                bugzillaBugBuilder.targetMilestone(issue.getTargetMilestone());
            }

            if (issue.getTargetRelease() != null) {
                bugzillaBugBuilder.targetRelease(issue.getTargetRelease());
            }

            BugzillaBug bugzillaBug = bugzillaBugBuilder.build();

            logger.debug("Bug found: {} - {}/{} - {} - {} - {}",
                    issue.getId(),
                    bugzillaBug.getPriority().getSymbol(),
                    bugzillaBug.getSeverity().getSymbol(),
                    bugzillaBug.getStatus(),
                    issue.getAssignedTo(),
                    issue.getSummary());

            userMatchingService.getByBugzilla(issue.getAssignedTo()).ifPresent(
                    u -> {
                        logger.debug("Bug {} ({}) assigned to {}", issue.getId(),
                                bugzillaBug.getBug().getId(),
                                u.getName());
                        bugzillaBug.setAssignedTo(u);
                    }
            );

            kiBugs.put(bugzillaBug.getId(), bugzillaBug);
        }

        return kiBugs;
    }

    public static AtomicBoolean getFinished() {
        return finished;
    }
}
