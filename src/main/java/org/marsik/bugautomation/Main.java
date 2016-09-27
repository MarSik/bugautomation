package org.marsik.bugautomation;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletException;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.marsik.bugautomation.jobs.BugzillaRefreshJob;
import org.marsik.bugautomation.jobs.TrelloRefreshJob;
import org.marsik.bugautomation.services.QuartzService;
import org.marsik.bugautomation.jobs.RefreshRulesJob;
import org.marsik.bugautomation.server.RestServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Inject
    QuartzService scheduler;

    QuartzService.Timer ruleTimer;
    QuartzService.Timer bzTimer;
    QuartzService.Timer trelloTimer;

    @PostConstruct
    public void create() {
        ruleTimer = scheduler.createTimer(30, RefreshRulesJob.class);
        trelloTimer = scheduler.createTimer(120, TrelloRefreshJob.class);
        bzTimer = scheduler.createTimer(600, BugzillaRefreshJob.class);
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public static void main(String[] args) {
        final Weld w = new Weld();
        final WeldContainer wc = w.initialize();

        // Make sure the app is instantiated
        Main bean = wc.instance().select(Main.class).get();
        bean.toString();

        // Start REST server
        try {
            RestServer.build(8080);
        } catch (ServletException ex) {
            logger.error("Server failed", ex);
        }
    }
}
