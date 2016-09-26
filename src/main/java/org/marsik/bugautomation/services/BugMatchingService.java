package org.marsik.bugautomation.services;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;

import org.marsik.bugautomation.facts.Bug;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

@Singleton
public class BugMatchingService {
    AtomicInteger nextId = new AtomicInteger();
    BiMap<String, Bug> bzIdToBug = HashBiMap.create();

    private static final Pattern RE_RHBZ = Pattern.compile("\\[?(bug *#?|show_bug.cgi?id=|(rh)?(bz)?#?)?(?<id>[1-9][0-9]{5,6})\\]?");

    public Optional<Bug> identifyBug(String text) {
        final Matcher matcher = RE_RHBZ.matcher(text.toLowerCase());
        while (matcher.find()) {
            String bugId = matcher.group("id");
            Bug bug = getBugByBzId(bugId);

            return Optional.of(bug);
        }

        return Optional.empty();
    }

    public Bug getBugByBzId(String bugId) {
        Bug bug = bzIdToBug.get(bugId);
        if (bug == null) {
            bug = new Bug(nextId.getAndIncrement());
            bzIdToBug.put(bugId, bug);
        }
        return bug;
    }

    public String getBzBug(Bug bug) {
        return bzIdToBug.inverse().get(bug);
    }
}
