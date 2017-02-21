package org.marsik.bugautomation.services;

import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.io.IOUtils;

@Singleton
public class ConfigurationServiceProperties implements ConfigurationService {

    private LoadingCache<String, String> cache;

    public ConfigurationServiceProperties() {
        cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build(new CacheLoader<String, String>() {
                public String load(@NotNull String key) {
                    return get(key).orElse(null);
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
            release = getCached("release.before." + release);
            if (release != null) {
                releases.add(release);
            }
        } while (release != null);

        return releases;
    }
}
