package org.marsik.bugautomation.jobs;

import javax.inject.Inject;

import org.kie.api.cdi.KSession;
import org.kie.api.runtime.KieSession;
import org.marsik.bugautomation.services.BugzillaActions;
import org.marsik.bugautomation.services.TrelloActions;
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
    BugzillaActions bugzillaActions;

    @Inject
    TrelloActions trelloActions;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("Triggering rules...");
        kSession.setGlobal("bugzilla", bugzillaActions);
        kSession.setGlobal("trello", trelloActions);
        kSession.fireAllRules();
    }
}
