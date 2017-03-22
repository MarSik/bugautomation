package org.marsik.bugautomation.services;

import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.io.IOUtils;
import org.marsik.bugautomation.facts.TrelloBoard;

@Singleton
public class ConfigurationServiceProperties implements ConfigurationService {

    private LoadingCache<String, String> cache;
    private LoadingCache<String, Boolean> monitoredBoards;

    public ConfigurationServiceProperties() {
        cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build(new CacheLoader<String, String>() {
                public String load(@NotNull String key) {
                    return get(key).orElse(null);
                }
            });

        monitoredBoards = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Boolean>() {
                    public Boolean load(@NotNull String key) {
                        return checkBoardMonitored(key);
                    }
                });
    }

    @Override
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
    @Override
    public String getCached(String key) {
        try {
            return cache.getUnchecked(key);
        } catch (CacheLoader.InvalidCacheLoadException ex) {
            // When no such entry exists
            return null;
        }
    }

    public Optional<String> getCachedOptional(String key) {
        try {
            return Optional.of(cache.getUnchecked(key));
        } catch (CacheLoader.InvalidCacheLoadException ex) {
            // When no such entry exists
            return Optional.empty();
        }
    }

    @Override
    public Integer getCachedInt(String key, Integer def) {
        final String value = getCached(key);
        if (value != null) {
            return Integer.valueOf(value);
        } else {
            return def;
        }
    }

    @Override
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

    @Override
    public String resolveRelease(String release) {
        final String mappedRelease = getCached("release.map." + release);
        return mappedRelease != null ? mappedRelease : release;
    }

    @Override
    public Set<String> findAllReleases(String release) {
        Set<String> releases = new HashSet<>();
        releases.add(release);

        do {
            release = getCached("release.fixbefore." + release);
            if (release != null) {
                releases.add(release);
            }
        } while (release != null);

        return releases;
    }

    public List<String> getMonitoredBoards() {
        return Arrays.asList(
                Optional.ofNullable(
                        getCached(ConfigurationService.TRELLO_BOARDS)
                ).orElse("").split(" *, *"));
    }

    public boolean checkBoardMonitored(String id) {
        return getCachedOptional(ConfigurationService.TRELLO_BOARDS)
                .map(s -> s.split(" *, *"))
                .map(Arrays::asList)
                .map(HashSet::new)
                .map(s -> s.contains(id))
                .orElse(false);
    }

    @Override
    public boolean isBoardMonitored(String id) {
        return monitoredBoards.getUnchecked(id);
    }

    @Override
    public String getBacklog(TrelloBoard board) {
        final String backlog = getCached("cfg.backlog." + board.getId());
        return backlog == null ? "todo" : backlog.toLowerCase();
    }

    @Override
    public String getDonelog(TrelloBoard board) {
        final String backlog = getCached("cfg.done." + board.getId());
        return backlog == null ? "done" : backlog.toLowerCase();
    }
}
