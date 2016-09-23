package org.marsik.bugautomation.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.kie.api.cdi.KSession;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

@Singleton
public class FactService {
    @Inject
    @KSession("bug-rules")
    KieSession kSession;

    Map<Object, FactHandle> handles = new ConcurrentHashMap<>();

    public void addOrUpdateFact(Object o) {
        if (handles.containsKey(o)) {
            updateFact(o, o);
        } else {
            addFact(o);
        }
    }

    public void addFact(Object o) {
        FactHandle handle = kSession.insert(o);
        handles.put(o, handle);
    }

    public void removeFact(Object o) {
        kSession.delete(handles.get(o));
        handles.remove(o);
    }

    public void updateFact(Object oldValue, Object newValue) {
        kSession.update(handles.get(oldValue), newValue);
        if (!Objects.equals(newValue, oldValue)) {
            handles.put(newValue, handles.get(oldValue));
            handles.remove(oldValue);
        }
    }
}
