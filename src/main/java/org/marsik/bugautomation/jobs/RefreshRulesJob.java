package org.marsik.bugautomation.jobs;

import javax.inject.Inject;

import org.kie.api.cdi.KSession;
import org.kie.api.runtime.KieSession;
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

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("Triggering rules...");
        kSession.fireAllRules();
    }
}
