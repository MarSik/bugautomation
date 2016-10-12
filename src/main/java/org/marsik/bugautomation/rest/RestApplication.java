package org.marsik.bugautomation.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class RestApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        final HashSet<Class<?>> classes = new HashSet<>();
        classes.add(MetricsEndpoint.class);
        return classes;
    }
}
