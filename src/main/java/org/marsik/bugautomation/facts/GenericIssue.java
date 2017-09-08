package org.marsik.bugautomation.facts;

import java.util.Collection;
import java.util.Optional;

import org.marsik.bugautomation.services.ConfigurationService;

public interface GenericIssue {
    Bug getBug();
    String getTitle();
    String getDescription();

    Collection<User> getAssignedUsers();

    boolean isDone();
    boolean isUntargeted();
    boolean isTargeted();

    /** Unique identifier used to find this issue again
        whatever source it comes from
    */
    String getUid();

    Optional<String> getUrl(ConfigurationService configuration);

    /** Short version of id for card title
     */
    String getTitleId();
}
