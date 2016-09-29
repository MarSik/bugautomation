package org.marsik.bugautomation.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.IOUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

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

    private LoadingCache<String, String> cache;

    public ConfigurationService() {
        cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build(new CacheLoader<String, String>() {
                public String load(@NotNull String key) {
                    return get(key).orElse(null);
                }
            });
    }

    public Optional<String> get(String key) {
        Properties config = getProperties();

        return config.containsKey(key) ? Optional.of(config.getProperty(key)) : Optional.empty();
    }

    /**
     * Use this method when no immediate config file refresh is desired.
     * It should always be used from DRL files.
     *
     * @param key config key to retrieve
     * @return value or null
     */
    public String getCached(String key) {
         return cache.getUnchecked(key);
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
