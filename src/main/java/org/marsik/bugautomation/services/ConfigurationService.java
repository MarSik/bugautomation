package org.marsik.bugautomation.services;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.marsik.bugautomation.facts.TrelloBoard;

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
    Set<String> findAllReleases(String release);

    public List<String> getMonitoredBoards();
    boolean isBoardMonitored(String id);
    String getBacklog(TrelloBoard board);
    String getDonelog(TrelloBoard board);
}
