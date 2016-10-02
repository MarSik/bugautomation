package org.marsik.bugautomation.services;

import javax.inject.Singleton;

import org.marsik.bugautomation.facts.BugzillaBug;
import org.marsik.bugautomation.facts.User;

@Singleton
public class BugzillaActionsImpl implements BugzillaActions {
    @Override
    public void assignTo(BugzillaBug bug, User user) {

    }
}
