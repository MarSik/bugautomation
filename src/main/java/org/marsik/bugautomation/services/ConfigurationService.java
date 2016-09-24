package org.marsik.bugautomation.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;

@Singleton
public class ConfigurationService {
    public static final String TRELLO_APP_KEY = "trello.appkey";
    public static final String TRELLO_TOKEN = "trello.token";
    public static final String TRELLO_BOARDS = "trello.boards";

    public static final String BUGZILLA_URL = "bugzilla.url";
    public static final String BUGZILLA_OWNERS = "bugzilla.owners";
    public static final String BUGZILLA_TEAMS = "bugzilla.teams";

    public static final String BUGZILLA_USERNAME = "bugzilla.username";
    public static final String BUGZILLA_PASSWORD = "bugzilla.password";

    public Optional<String> get(String key) {
        Properties config = getProperties();

        return config.containsKey(key) ? Optional.of(config.getProperty(key)) : Optional.empty();
    }

    public Properties getProperties() {
        Properties config = new Properties();

        String cfgFile = System.getProperty("bug.config");
        if (cfgFile != null
                && new File(cfgFile).exists()) {

            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(cfgFile);
                config.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        return config;
    }
}
