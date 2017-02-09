package org.marsik.bugautomation.jobs;

import javax.inject.Inject;

import org.marsik.bugautomation.services.BugMatchingService;
import org.marsik.bugautomation.services.ConfigurationService;
import org.marsik.bugautomation.services.FactService;
import org.marsik.bugautomation.services.UserMatchingService;

/**
 * Use https://github.com/uwolfer/gerrit-rest-java-client
 */
public class GerritRefreshJob implements Runnable {
    @Inject
    FactService factService;

    @Inject
    ConfigurationService configurationService;

    @Inject
    UserMatchingService userMatchingService;

    @Inject
    BugMatchingService bugMatchingService;

    @Override
    public void run() {

    }
}
