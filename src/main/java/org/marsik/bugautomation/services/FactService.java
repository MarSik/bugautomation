package org.marsik.bugautomation.services;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.kie.api.cdi.KSession;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

@Singleton
public class FactService {
    @Inject
    @KSession("bug-rules")
    KieSession kSession;

    Map<Object, FactHandle> handles = new ConcurrentHashMap<>();

    public void addOrUpdateFact(@NotNull Object o) {
        if (handles.containsKey(o)) {
            updateFact(o, o);
        } else {
            addFact(o);
        }
    }

    public void addFact(@NotNull Object o) {
        FactHandle handle = kSession.insert(o);
        handles.put(o, handle);
    }

    public void removeFact(@NotNull Object o) {
        if (handles.containsKey(o)) {
            kSession.delete(handles.get(o));
            handles.remove(o);
        }
    }

    public void updateFact(@NotNull Object oldValue, @NotNull Object newValue) {
        kSession.update(handles.get(oldValue), newValue);
        if (!Objects.equals(newValue, oldValue)) {
            handles.put(newValue, handles.get(oldValue));
            handles.remove(oldValue);
        }
    }

    public void clear() {
        kSession.getFactHandles().stream().forEach(kSession::delete);
        handles.clear();
    }
}
