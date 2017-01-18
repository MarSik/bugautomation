package org.marsik.bugautomation.services;

import java.util.Optional;
import java.util.Properties;

public interface ConfigurationService {
    String TRELLO_APP_KEY = "trello.appkey";
    String TRELLO_TOKEN = "trello.token";
    String TRELLO_BOARDS = "trello.boards";
    String BUGZILLA_URL = "bugzilla.url";
    String BUGZILLA_OWNERS = "bugzilla.owners";
    String BUGZILLA_TEAMS = "bugzilla.teams";
    String BUGZILLA_USERNAME = "bugzilla.username";
    String BUGZILLA_PASSWORD = "bugzilla.password";

    Optional<String> get(String key);

    String getCached(String key);
    Integer getCachedInt(String key, Integer def);

    Properties getProperties();

    String resolveRelease(String release);
}
