package org.marsik.bugautomation.services;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Singleton;

import org.marsik.bugautomation.facts.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

@Singleton
public class UserMatchingService {
    private static final Logger log = LoggerFactory.getLogger(UserMatchingService.class);

    AtomicInteger nextId = new AtomicInteger();
    BiMap<Integer, User> users = HashBiMap.create();

    Map<String, User> emailToUser = new HashMap<>();
    Map<String, User> usernameToUser = new HashMap<>();

    public User createUser(String name, Collection<String> usernames, Collection<String> emails) {
        User user = User.builder()
                .name(name)
                .id(nextId.getAndIncrement())
                .build();

        users.put(user.getId(), user);

        for (String username: usernames) {
            usernameToUser.put(username, user);
        }

        for (String email: emails) {
            emailToUser.put(email, user);
        }

        return user;
    }

    public Optional<User> getByEmail(String email) {
        User user = emailToUser.get(email);

        if (user == null) {
            log.warn("Unknown user identified just by email {} - matching will not work", email);
            user = createUser(email, Collections.emptyList(), Collections.singletonList(email));
        }

        return user == null ? Optional.empty() : Optional.of(user);
    }

    public Optional<User> getByUsername(String username) {
        User user = usernameToUser.get(username);

        if (user == null) {
            log.warn("Unknown user identified just by username {} - matching will not work", username);
            user = createUser(username, Collections.singletonList(username), Collections.emptyList());
        }

        return user == null ? Optional.empty() : Optional.of(user);
    }
}
