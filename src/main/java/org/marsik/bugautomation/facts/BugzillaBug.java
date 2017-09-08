package org.marsik.bugautomation.facts;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;

import org.hibernate.validator.constraints.NotEmpty;
import org.marsik.bugautomation.services.ConfigurationService;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "uid")
@Builder
public class BugzillaBug implements GenericIssue {
    String id;

    @NotNull
    @NotEmpty
    String uid;

    String title;
    String description;

    /**
     * What community does this bug belong to? oVirt, Red Hat or other?
     */
    @NotNull
    String community;

    @NotNull
    BugzillaStatus status;
    LocalDateTime statusModifiedAt;

    @NotNull
    BugzillaPriorityLevel priority;
    @NotNull
    BugzillaPriorityLevel severity;

    /**
     * PM score - higher numbers have to be finished first
     */
    @NotNull
    Integer pmScore;

    /**
     * PM priority - lower numbers have to be finished first
     */
    @NotNull
    Integer pmPriority;

    @NotNull
    String targetMilestone;
    String targetRelease;

    Set<String> verified;

    @NotNull
    Set<BugzillaBugFlag> flags;
    @NotNull
    Set<String> keywords;
    @NotNull
    Set<Bug> blocks;
    @NotNull
    User assignedTo;
    @NotNull
    Bug bug;

    public boolean isDone() {
        return BugzillaStatus.MODIFIED.compareTo(status) <= 0;
    }

    public boolean isUntargeted() {
        return targetMilestone == null || targetMilestone.isEmpty();
    }

    public boolean isTargeted() {
        return !isUntargeted();
    }

    @Override
    public Collection<User> getAssignedUsers() {
        return Collections.singletonList(assignedTo);
    }

    @Override
    public Optional<String> getUrl(ConfigurationService configuration) {
        final Optional<String> bugzillaUrl = configuration.get(ConfigurationService.BUGZILLA_URL);
        if (bugzillaUrl.isPresent()) {
            URI uri = UriBuilder.fromUri(bugzillaUrl.get())
                    .segment(bug.getId())
                    .build();
            return Optional.of(uri.toString());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String getTitleId() {
        return "bz#" + getId();
    }
}
