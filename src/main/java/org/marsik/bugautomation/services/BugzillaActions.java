package org.marsik.bugautomation.services;

import org.marsik.bugautomation.facts.BugzillaBug;
import org.marsik.bugautomation.facts.User;

public interface BugzillaActions {
    void assignTo(BugzillaBug bug, User user);
}
