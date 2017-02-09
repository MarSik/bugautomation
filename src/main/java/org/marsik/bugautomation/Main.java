package org.marsik.bugautomation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.ServletException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.marsik.bugautomation.jobs.BugzillaRefreshJob;
import org.marsik.bugautomation.jobs.RefreshRulesJob;
import org.marsik.bugautomation.jobs.TrelloRefreshJob;
import org.marsik.bugautomation.server.RestServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private ScheduledExecutorService scheduler;
    private ExecutorService threadPool;

    @Inject
    RefreshRulesJob refreshRulesJob;

    @Inject
    TrelloRefreshJob trelloRefreshJob;

    @Inject
    BugzillaRefreshJob bugzillaRefreshJob;

    @PostConstruct
    public void create() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        threadPool = Executors.newCachedThreadPool();

        scheduler.scheduleWithFixedDelay(() -> threadPool.submit(refreshRulesJob),
                0, 30, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(() -> threadPool.submit(trelloRefreshJob),
                0, 120, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(() -> threadPool.submit(bugzillaRefreshJob),
                0, 300, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void tearDown() {
        scheduler.shutdown();
        threadPool.shutdown();
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
            RestServer.build(8080, wc.getBeanManager());
        } catch (ServletException ex) {
            logger.error("Server failed", ex);
        }
    }
}
