package org.marsik.bugautomation.services;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.marsik.bugautomation.facts.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UserMatchingService {
    private static final Logger log = LoggerFactory.getLogger(UserMatchingService.class);

    BiMap<String, User> users = HashBiMap.create();

    BiMap<String, User> bugzillaToUser = HashBiMap.create();
    BiMap<String, User> trelloToUser = HashBiMap.create();
    BiMap<String, User> githubToUser = HashBiMap.create();

    @Inject
    ConfigurationService configurationService;

    @PostConstruct
    public void loadUsers() {
        loadUsers(configurationService.getProperties());
    }

    public User createUser(String name, Collection<String> usernames, Collection<String> emails, Collection<String> githubNicks) {
        User user = new User(name);

        users.put(user.getName(), user);

        for (String username: usernames) {
            trelloToUser.put(username, user);
        }

        for (String email: emails) {
            bugzillaToUser.put(email, user);
        }

        for (String gname: githubNicks) {
            githubToUser.put(gname.toLowerCase(), user);
        }

        return user;
    }

    public Optional<User> getByBugzilla(String email) {
        User user = bugzillaToUser.get(email);

        if (user == null) {
            log.warn("Unknown user identified just by bugzilla {} - matching will not work", email);
            user = createUser("bz:"+email, Collections.emptyList(), Collections.singletonList(email), Collections.emptyList());
        }

        return Optional.of(user);
    }

    public Optional<User> getByTrello(String username) {
        User user = trelloToUser.get(username);

        if (user == null) {
            log.warn("Unknown user identified just by trello {} - matching will not work", username);
            user = createUser("tr:"+username, Collections.singletonList(username), Collections.emptyList(), Collections.emptyList());
        }

        return Optional.of(user);
    }

    public Optional<User> getByGithub(String username) {
        User user = githubToUser.get(username.toLowerCase());

        if (user == null) {
            log.warn("Unknown user identified just by GitHub {} - matching will not work", username);
            user = createUser("gh:"+username, Collections.emptyList(), Collections.emptyList(), Collections.singletonList(username));
        }

        return Optional.of(user);
    }

    public Optional<String> getTrello(User user) {
        final BiMap<User, String> userToTrello = trelloToUser.inverse();
        return userToTrello.containsKey(user) ? Optional.of(userToTrello.get(user)) : Optional.empty();
    }

    public Optional<String> getBugzilla(User user) {
        final BiMap<User, String> userToBz = bugzillaToUser.inverse();
        return userToBz.containsKey(user) ? Optional.of(userToBz.get(user)) : Optional.empty();
    }

    public Optional<String> getGithub(User user) {
        final BiMap<User, String> userToGh = githubToUser.inverse();
        return userToGh.containsKey(user) ? Optional.of(userToGh.get(user)) : Optional.empty();
    }

    public List<User> loadUsers(Properties config) {
        List<String> usernames = getUserNames(config);
        List<User> users = new ArrayList<>();

        for (String username: usernames) {
            String bzEmail = config.getProperty("user." + username + ".bugzilla");
            String trId = config.getProperty("user." + username + ".trello");
            String ghNicks = config.getProperty("user." + username + ".github");

            User user = createUser(username,
                    parseNames(trId),
                    parseNames(bzEmail),
                    parseNames(ghNicks));

            log.info("Loaded user {} - BZ:{}, Trello:{}",
                    user.getName(),
                    getBugzilla(user).orElse("-none-"),
                    getTrello(user).orElse("-none-"));

            users.add(user);
        }

        return users;
    }

    public List<String> getUserNames(Properties config) {
        return config.stringPropertyNames().stream()
                .filter(name -> name.startsWith("user."))
                .map(name -> name.substring(5))
                .map(name -> name.replaceFirst("\\..*", ""))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> parseNames(String name) {
        if (name == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(name);
    }
}
