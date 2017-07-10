package org.marsik.bugautomation.services;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import org.marsik.bugautomation.facts.BugzillaBug;
import org.marsik.bugautomation.facts.User;

@ApplicationScoped
public class BugzillaActionsImpl implements BugzillaActions {
    @Override
    public void assignTo(BugzillaBug bug, User user) {

    }
}
