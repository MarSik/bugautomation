package org.marsik.bugautomation;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.servlet.ServletException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.kie.api.runtime.KieSession;
import org.marsik.bugautomation.jobs.BugzillaRefreshJob;
import org.marsik.bugautomation.jobs.GithubRefreshJob;
import org.marsik.bugautomation.jobs.RefreshRulesJob;
import org.marsik.bugautomation.jobs.TrelloRefreshJob;
import org.marsik.bugautomation.rules.RuleLoader;
import org.marsik.bugautomation.server.RestServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private ScheduledExecutorService scheduler;

    @Produces
    @ApplicationScoped
    public KieSession initSession() throws IOException {
        return RuleLoader.loadKieBase("rules", null)
                .newKieSession();
    }

    @Inject
    RefreshRulesJob refreshRulesJob;

    @Inject
    TrelloRefreshJob trelloRefreshJob;

    @Inject
    BugzillaRefreshJob bugzillaRefreshJob;

    @Inject
    GithubRefreshJob githubRefreshJob;

    @PostConstruct
    public void create() {
        scheduler = Executors.newScheduledThreadPool(3);

        scheduler.scheduleWithFixedDelay(new SafeRunnable(refreshRulesJob),
                0, 30, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(new SafeRunnable(trelloRefreshJob),
                0, 120, TimeUnit.SECONDS);
        // scheduler.scheduleWithFixedDelay(new SafeRunnable(githubRefreshJob),
        //        0, 15*60, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(new SafeRunnable(bugzillaRefreshJob),
                0, 300, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void tearDown() {
        scheduler.shutdown();
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

    @AllArgsConstructor
    private static class SafeRunnable implements Runnable {
        private final Runnable task;

        @Override
        public void run() {
            try {
                task.run();
            } catch (Exception ex) {
                logger.error("The task {} failed with exception", task.getClass().getName(), ex);
            }
        }
    }
}
