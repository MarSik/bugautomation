package org.marsik.bugautomation.services;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;

import org.marsik.bugautomation.stats.Stats;

@Singleton
public class StatsService {
    private AtomicReference<Stats> stats = new AtomicReference<>();

    public StatsService() {
        stats.set(new Stats());
    }

    public Stats getStats() {
        return stats.get();
    }

    public void setStats(Stats stats) {
        this.stats.set(stats);
    }
}
