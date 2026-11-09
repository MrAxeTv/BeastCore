package me.mraxetv.beastlib.utils;

import me.mraxetv.beastlib.BeastLib;
import me.mraxetv.beastlib.api.scheduler.SchedulerServices;
import me.mraxetv.beastlib.api.scheduler.SchedulerTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Correctly named convenience facade for tasks owned by BeastLib itself.
 *
 * <p>Dependent plugins should normally call {@code getScheduler()} from their
 * BeastLib API implementation and pass their own plugin as the owner. That
 * allows BeastLib to cancel their tasks independently during plugin disable.</p>
 */
public final class SchedulerHandler {

    public static final long DEFAULT_DELAY_TICKS = 1200L;

    private SchedulerHandler() {
    }

    public static SchedulerTask runGlobal(Runnable runnable) {
        return SchedulerServices.get().runGlobal(plugin(), runnable);
    }

    public static SchedulerTask runGlobalDelayed(Runnable runnable, long delayTicks) {
        return SchedulerServices.get().runGlobalDelayed(plugin(), runnable, delayTicks);
    }

    public static SchedulerTask runGlobalRepeating(
            Runnable runnable,
            long firstRunDelayTicks,
            long periodTicks
    ) {
        return SchedulerServices.get().runGlobalRepeating(
                plugin(),
                runnable,
                firstRunDelayTicks,
                periodTicks
        );
    }

    public static SchedulerTask runAsync(Runnable runnable) {
        return SchedulerServices.get().runAsync(plugin(), runnable);
    }

    public static SchedulerTask runAsyncDelayed(Runnable runnable, long delayTicks) {
        return SchedulerServices.get().runAsyncDelayed(plugin(), runnable, delayTicks);
    }

    public static SchedulerTask runAsyncRepeating(
            Runnable runnable,
            long firstRunDelayTicks,
            long periodTicks
    ) {
        return SchedulerServices.get().runAsyncRepeating(
                plugin(),
                runnable,
                firstRunDelayTicks,
                periodTicks
        );
    }

    public static SchedulerTask runEntity(Entity entity, Runnable runnable) {
        return SchedulerServices.get().runEntity(plugin(), entity, runnable);
    }

    public static SchedulerTask runEntityDelayed(
            Entity entity,
            Runnable runnable,
            long delayTicks
    ) {
        return SchedulerServices.get().runEntityDelayed(plugin(), entity, runnable, delayTicks);
    }

    public static SchedulerTask runEntityRepeating(
            Entity entity,
            Runnable runnable,
            long firstRunDelayTicks,
            long periodTicks
    ) {
        return SchedulerServices.get().runEntityRepeating(
                plugin(),
                entity,
                runnable,
                firstRunDelayTicks,
                periodTicks
        );
    }

    public static SchedulerTask runRegion(Location location, Runnable runnable) {
        return SchedulerServices.get().runRegion(plugin(), location, runnable);
    }

    public static SchedulerTask runRegionDelayed(
            Location location,
            Runnable runnable,
            long delayTicks
    ) {
        return SchedulerServices.get().runRegionDelayed(plugin(), location, runnable, delayTicks);
    }

    public static SchedulerTask runRegionRepeating(
            Location location,
            Runnable runnable,
            long firstRunDelayTicks,
            long periodTicks
    ) {
        return SchedulerServices.get().runRegionRepeating(
                plugin(),
                location,
                runnable,
                firstRunDelayTicks,
                periodTicks
        );
    }

    public static SchedulerTask runSync(Runnable runnable) {
        return runGlobal(runnable);
    }

    public static SchedulerTask runSyncDelayed(Runnable runnable, long delayTicks) {
        return runGlobalDelayed(runnable, delayTicks);
    }

    public static SchedulerTask runSyncScheduled(
            Runnable runnable,
            long firstRunDelayTicks,
            long periodTicks
    ) {
        return runGlobalRepeating(runnable, firstRunDelayTicks, periodTicks);
    }

    public static SchedulerTask runAsyncScheduled(
            Runnable runnable,
            long firstRunDelayTicks,
            long periodTicks
    ) {
        return runAsyncRepeating(runnable, firstRunDelayTicks, periodTicks);
    }

    private static Plugin plugin() {
        Plugin plugin = BeastLib.getInstance();
        if (plugin == null) {
            throw new IllegalStateException("BeastLib instance is not available yet.");
        }
        return plugin;
    }
}
