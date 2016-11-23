package org.marsik.bugautomation.services;

import javax.inject.Singleton;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.marsik.bugautomation.facts.Bug;

@Singleton
public class BugMatchingService {
    BiMap<String, WeakReference<Bug>> bzIdToBug = HashBiMap.create();

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
        WeakReference<Bug> weakBug = bzIdToBug.get(bugId);
        Bug bug = weakBug == null ? null : weakBug.get();

        if (bug == null) {
            bug = new Bug(bugId);
            weakBug = new WeakReference<>(bug);
            bzIdToBug.put(bugId, weakBug);
        }

        return bug;
    }

    /*public String getBzBug(Bug bug) {
        return bzIdToBug.inverse().get(bug);
    }*/

    public Set<String> getKnownBugs() {
        return bzIdToBug.keySet();
    }
}
