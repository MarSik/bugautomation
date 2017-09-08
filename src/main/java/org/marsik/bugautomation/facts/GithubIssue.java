package org.marsik.bugautomation.facts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.marsik.bugautomation.services.ConfigurationService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "uid")
@AllArgsConstructor
@Builder
public class GithubIssue implements GenericIssue {
    @NotNull
    Integer id;

    @NotNull
    @NotEmpty
    String uid;

    Bug bug;

    String title;
    String description;

    String githubUrl;

    @NotNull
    @NotEmpty
    String repoOwner;
    @NotNull
    @NotEmpty
    String repo;

    @Builder.Default
    List<User> assignedTo = new ArrayList<>();

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean isUntargeted() {
        return true;
    }

    @Override
    public boolean isTargeted() {
        return false;
    }

    @Override
    public Collection<User> getAssignedUsers() {
        return Collections.unmodifiableList(assignedTo);
    }

    @Override
    public Optional<String> getUrl(ConfigurationService configuration) {
        return Optional.of(getGithubUrl());
    }

    @Override
    public String getTitleId() {
        return getRepo() + "#" + getId().toString();
    }
}
