package org.marsik.bugautomation.jobs;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import org.marsik.bugautomation.services.RuleGlobalsService;
import org.marsik.bugautomation.services.StatsService;
import org.marsik.bugautomation.services.UserMatchingService;
import org.marsik.bugautomation.stats.SingleStat;
import org.marsik.bugautomation.stats.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BugzillaRefreshJob implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(BugzillaRefreshJob.class);
    private static final AtomicBoolean finished = new AtomicBoolean(false);

    private static final Pattern PM_PRIO_WHITEBOARD_RE = Pattern.compile("PM-(?<score>[0-9]+)", Pattern.CASE_INSENSITIVE);

    @Inject
    FactService factService;

    @Inject
    ConfigurationService configurationService;

    @Inject
    UserMatchingService userMatchingService;

    @Inject
    BugMatchingService bugMatchingService;

    @Inject
    StatsService statsService;

    @Inject
    RuleGlobalsService ruleGlobalsService;

    // Record the last changed time of a bug we retrieved
    // to not retrieve it again when no change happened
    private Map<String, Date> changedMap = new HashMap<>();

    @Override
    public void run() {
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

        Set<String> bugIds = new HashSet<>(); // updated bugs
        Map<String, BugzillaBug> retrievedBugs = new HashMap<>();

        if (session.open()) {
            logger.info("Refreshing bugzilla bugs");
            long startTime = System.nanoTime();

            // Remember the full list temporarily to keep them in the fact database
            Set<String> allKnownBugs = new HashSet<>();

            // Search bugs by users
            Multimap<String, Object> searchData = ArrayListMultimap.create();
            if (bugzillaOwners.isPresent() && !bugzillaOwners.get().trim().isEmpty()) {
                for (String owner : splitNames(bugzillaOwners.get())) {
                    searchData.put("assigned_to", owner);
                }
                populateSearchData(searchData);
                Iterable<BugProxy> i = session.searchBugs(searchData);
                i.forEach(b -> allKnownBugs.add(b.getId()));
                consumeIfNewer(i, bug -> bugIds.add(bug.getId()));
            }

            // Search bugs by teams
            if (bugzillaTeams.isPresent() && !bugzillaTeams.get().trim().isEmpty()) {
                searchData = ArrayListMultimap.create();
                for (String team : splitNames(bugzillaTeams.get())) {
                    searchData.put("cf_ovirt_team", team);
                }
                populateSearchData(searchData);
                Iterable<BugProxy> i = session.searchBugs(searchData);
                i.forEach(b -> allKnownBugs.add(b.getId()));
                consumeIfNewer(i, bug -> bugIds.add(bug.getId()));
            }

            // Retrieve all changed bugs in chunks
            while (!bugIds.isEmpty()) {
                List<String> chunk = bugIds.stream().limit(100).collect(Collectors.toList());
                bugIds.removeAll(chunk);
                retrievedBugs.putAll(retrieveAndProcess(session, chunk));

                // Load flags
                for (BugProxy bzExtra: session.getExtra(chunk)) {
                    bzExtra.loadFlags(bzExtra);
                    retrievedBugs.get(bzExtra.getId()).setFlags(bzExtra.getFlags());
                }
            }

            logger.info("Retrieved {} changed bugs out of {} total.",
                    retrievedBugs.size(), allKnownBugs.size());

            // Close the session
            session.close();

            // Update fact database
            retrievedBugs.values().stream().forEach(factService::addOrUpdateFact);

            // Forget about bugs that were assigned out of scope
            Collection<BugzillaBug> bugsToRemove = ruleGlobalsService.getBugzillaBugs();
            bugsToRemove = bugsToRemove.stream()
                    .filter(b -> !allKnownBugs.contains(b.getId()))
                    .collect(Collectors.toList());

            logger.info("Forgetting about bugs: {}", bugsToRemove.stream()
                    .map(BugzillaBug::getId).collect(Collectors.toList()));

            bugsToRemove.stream()
                    .peek(b -> changedMap.remove(b.getId()))
                    .forEach(factService::removeFact);

            finished.set(true);
            long elapsedTime = System.nanoTime() - startTime;

            final Stats stats = new Stats();
            stats.add(SingleStat.BUGS_REFRESH_TIME).value(elapsedTime);
            statsService.merge(stats);

            logger.info("Bugzilla refresh done ({} ms)", (float)elapsedTime / 1000000);
        }

    }

    private void consumeIfNewer(Iterable<BugProxy> list, Consumer<BugProxy> consumer) {
        StreamSupport.stream(list.spliterator(), false)
                .filter(bug -> !Objects.equals(bug.getLastChangeTime(), changedMap.get(bug.getId())))
                .peek(bug -> changedMap.put(bug.getId(), bug.getLastChangeTime()))
                .forEach(consumer);
    }

    private String[] splitNames(@NotNull String commaSeparatedNames) {
        return commaSeparatedNames.split(" *, *");
    }

    private void populateSearchData(Multimap<String, Object> searchData) {
        searchData.put("bug_status", "NEW");
        searchData.put("bug_status", "ASSIGNED");
        searchData.put("bug_status", "POST");
        searchData.put("bug_status", "MODIFIED");
        searchData.put("bug_status", "ON_QA");
        searchData.put("bug_status", "VERIFIED");

        searchData.put("include_fields", "id");
        searchData.put("include_fields", "last_change_time");
    }

    private Map<String, BugzillaBug> retrieveAndProcess(BugzillaClient session, List<String> bugIds) {
        Map<String, BugzillaBug> kiBugs = new HashMap<>();
        Iterable<BugProxy> i = session.getBugs(bugIds);
        for (BugProxy issue : i) {
             BugzillaBug.BugzillaBugBuilder bugzillaBugBuilder = BugzillaBug.builder()
                     .id(issue.getId())
                     .uid("bz-" + issue.getId())
                     .community(issue.getCommunity().toLowerCase().trim().replace(" ", ""))
                     .title(issue.getSummary())
                     .description(issue.getDescription())
                     .status(BugzillaStatus.valueOf(issue.getStatus().toUpperCase()))
                     .bug(bugMatchingService.getBugByBzId(issue.getId()))
                     .severity(BugzillaPriorityLevel.valueOf(issue.getSeverity().toUpperCase()))
                     .priority(BugzillaPriorityLevel.valueOf(issue.getPriority().toUpperCase()))
                     .pmScore(0)
                     .pmPriority(Integer.MAX_VALUE)
                     .verified(issue.getVerified().stream().map(String::toLowerCase).collect(Collectors.toSet()))
                     .keywords(issue.getKeywords().stream().map(String::toLowerCase).collect(Collectors.toSet()))
                     .blocks(issue.getBlocks().stream().map(bugMatchingService::getBugByBzId).collect(Collectors.toSet()));

            if (issue.getTargetMilestone() != null) {
                bugzillaBugBuilder.targetMilestone(configurationService.resolveRelease(issue.getTargetMilestone()));
            }

            if (issue.getTargetRelease() != null) {
                bugzillaBugBuilder.targetRelease(issue.getTargetRelease());
            }

            if (issue.getWhiteBoard() != null && !issue.getWhiteBoard().isEmpty()) {
                int highestPrio = Integer.MAX_VALUE;
                Matcher pmPrioWbMatch = PM_PRIO_WHITEBOARD_RE.matcher(issue.getWhiteBoard());
                while (pmPrioWbMatch.find()) {
                    highestPrio = Math.min(highestPrio, Integer.parseInt(pmPrioWbMatch.group("score")));
                }
                bugzillaBugBuilder.pmPriority(highestPrio);
            }

            /*
            Disable PM score temporarily, because there are old numbers in BZ
            and the PMs are not using this field ATM.

            if (issue.getPmScore() != null && !issue.getPmScore().isEmpty()) {
                final Integer pmScore = Integer.valueOf(issue.getPmScore());
                bugzillaBugBuilder.pmScore(pmScore);
            }
            */

            BugzillaBug bugzillaBug = bugzillaBugBuilder.build();

            logger.debug("Bug found: {} - {}/{} (prio: {}, sc: {}) - {} - {} - {}",
                    issue.getId(),
                    bugzillaBug.getPriority().getSymbol(),
                    bugzillaBug.getSeverity().getSymbol(),
                    bugzillaBug.getPmPriority(),
                    bugzillaBug.getPmScore(),
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
