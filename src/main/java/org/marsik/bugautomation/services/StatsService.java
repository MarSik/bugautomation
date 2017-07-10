package org.marsik.bugautomation.services;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;

import org.marsik.bugautomation.stats.Stats;

@ApplicationScoped
public class StatsService {
    private AtomicReference<Stats> stats = new AtomicReference<>();

    public StatsService() {
        stats.set(new Stats());
    }

    public synchronized Stats getStats() {
        return stats.get();
    }

    public synchronized void setStats(Stats stats) {
        this.stats.set(stats);
    }

    public synchronized void merge(Stats other) {
        stats.get().merge(other);
    }
}
