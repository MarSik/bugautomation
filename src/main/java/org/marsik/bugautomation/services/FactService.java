package org.marsik.bugautomation.services;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.kie.api.cdi.KSession;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

@ApplicationScoped
public class FactService {
    @Inject
    @KSession("bug-rules")
    KieSession kSession;

    public void addOrUpdateFact(@NotNull Object o) {
        kSession.submit((s) -> {
            FactHandle handle = s.getFactHandle(o);
            if (handle == null) {
                s.insert(o);
            } else {
                s.delete(handle);
                s.insert(o);
            }
        });
    }

    public void addOrUpdateFacts(@NotNull Collection<?> os) {
        kSession.submit((s) -> {
            for (Object o: os) {
                FactHandle handle = s.getFactHandle(o);
                if (handle == null) {
                    s.insert(o);
                } else {
                    s.delete(handle);
                    s.insert(o);
                }
            }
        });
    }

    public void addFact(@NotNull Object o) {
        kSession.submit((s) -> s.insert(o));
    }

    public void removeFact(@NotNull Object o) {
        kSession.submit((s) -> {
            FactHandle handle = s.getFactHandle(o);
            s.delete(handle);
        });
    }

    public void updateFact(@NotNull Object oldValue, @NotNull Object newValue) {
        kSession.submit((s) -> {
            FactHandle handle = s.getFactHandle(oldValue);
            s.update(handle, newValue);
        });
    }

    public void clear() {
        kSession.submit(s -> s.getFactHandles().stream().forEach(s::delete));
    }
}
