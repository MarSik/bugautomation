package org.marsik.bugautomation.jobs;

import javax.inject.Inject;

import org.kie.api.cdi.KSession;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.marsik.bugautomation.services.BugzillaActions;
import org.marsik.bugautomation.services.ConfigurationService;
import org.marsik.bugautomation.services.InternalActions;
import org.marsik.bugautomation.services.RuleGlobalsService;
import org.marsik.bugautomation.services.StatsService;
import org.marsik.bugautomation.services.TrelloActions;
import org.marsik.bugautomation.stats.SingleStat;
import org.marsik.bugautomation.stats.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshRulesJob implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RefreshRulesJob.class);

    @Inject
    @KSession("bug-rules")
    KieSession kSession;

    @Inject
    RuleGlobalsService ruleGlobalsService;

    @Inject
    StatsService statsService;

    @Override
    public void run() {
        if (!BugzillaRefreshJob.getFinished().get()
                || !TrelloRefreshJob.getFinished().get()) {
            logger.info("Delaying rules until the initial data collection finishes.");
            return;
        }

        logger.info("Triggering rules...");
        final Stats stats = new Stats();
        stats.add(SingleStat.TRIGGER_COUNT)
                .value(1f);
        FactHandle statsHandle = kSession.insert(stats);

        long startTime = System.nanoTime();

        if (logger.isDebugEnabled()) {
            kSession.addEventListener(new DebugAgendaEventListener());
        }

        kSession.fireAllRules();

        long elapsedTime = System.nanoTime() - startTime;
        stats.add(SingleStat.TRIGGER_TIME)
                .value((float) elapsedTime);

        kSession.delete(statsHandle);
        statsService.merge(stats);
        logger.info("All rules processed in {} ms", (float) elapsedTime / 1000000);
    }
}
