package org.marsik.bugautomation.jobs;

import javax.inject.Inject;

import org.kie.api.cdi.KSession;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.marsik.bugautomation.services.BugzillaActions;
import org.marsik.bugautomation.services.ConfigurationService;
import org.marsik.bugautomation.services.InternalActions;
import org.marsik.bugautomation.services.StatsService;
import org.marsik.bugautomation.services.TrelloActions;
import org.marsik.bugautomation.stats.SingleStat;
import org.marsik.bugautomation.stats.Stats;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisallowConcurrentExecution
public class RefreshRulesJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(RefreshRulesJob.class);

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

    @Inject
    StatsService statsService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!BugzillaRefreshJob.getFinished().get()
                || !TrelloRefreshJob.getFinished().get()) {
            logger.info("Delaying rules until the initial data collection finishes.");
            return;
        }

        logger.info("Triggering rules...");
        kSession.setGlobal("bugzilla", bugzillaActions);
        kSession.setGlobal("trello", trelloActions);
        kSession.setGlobal("config", configurationService);
        kSession.setGlobal("internal", internalActions);

        final Stats stats = new Stats(statsService.getStats());
        stats.add(SingleStat.TRIGGER_COUNT)
                .value(1f);
        FactHandle statsHandle = kSession.insert(stats);

        long startTime = System.nanoTime();
        kSession.fireAllRules();
        long elapsedTime = System.nanoTime() - startTime;
        stats.add(SingleStat.TRIGGER_TIME)
                .value((float) elapsedTime);

        kSession.delete(statsHandle);
        statsService.setStats(stats);
        logger.info("All rules processed in {} ms", (float)elapsedTime/1000000);
    }
}
