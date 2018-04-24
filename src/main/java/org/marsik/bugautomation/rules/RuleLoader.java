package org.marsik.bugautomation.rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.ResourceType;
import org.kie.internal.utils.KieHelper;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

@Slf4j
public class RuleLoader {
    public static KieBase loadKieBase(String resources, Path directory) throws IOException {
        KieServices kieServices = KieServices.Factory.get();

        // Enable Drools Fusion in Stream mode
        KieBaseConfiguration config = kieServices.newKieBaseConfiguration();
        config.setOption(EventProcessingOption.STREAM);
        config.setOption(EqualityBehaviorOption.EQUALITY);

        return loadAll(config, resources, directory);
    }

    public static KieBase loadAll(KieBaseConfiguration configuration, String classpath, Path directory) throws IOException {
        KieHelper kieHelper = new KieHelper();
        kieHelper.setClassLoader(ClassLoader.getSystemClassLoader());

        if (classpath != null) {
            getResourceFiles(classpath).stream()
                    .filter(p -> p.endsWith(".drl"))
                    .sorted()
                    .peek(f -> log.info("Loading internal rule file {}", f))
                    .forEach(kieHelper::addFromClassPath);
        }

        if (directory != null) {
            try {
                Files.list(directory)
                        .filter(p -> p.endsWith(".drl"))
                        .sorted()
                        .forEach(p -> {
                            try {
                                log.info("Loading external rule file {}", p);
                                kieHelper.addContent(Files.newBufferedReader(p)
                                                .lines()
                                                .collect(Collectors.joining()),
                                        ResourceType.DRL);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            } catch (NoSuchFileException ex) {
                log.warn("Additional rule files could not be loaded: {} {}", ex.getClass(), ex.getMessage());
            }
        }

        Results results = kieHelper.verify();
        if (results.hasMessages(Message.Level.ERROR)) {
            log.error("Could not parse DRL files: {}", results.getMessages());
            throw new IllegalStateException("DRL parsing errors");
        }

        return kieHelper.build(configuration);
    }

    private static Collection<String> getResourceFiles( String path ) throws IOException {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setScanners(new ResourcesScanner())
                        .setUrls(ClasspathHelper.forResource(path)));

        final Pattern pattern = Pattern.compile(".*\\.drl");
        Set<String> filenames = reflections.getResources(pattern).stream()
                .filter(p -> p.startsWith(path))
                .collect(Collectors.toSet());

        log.debug("Found the following rule files: {}", filenames);

        return filenames;
    }
}
