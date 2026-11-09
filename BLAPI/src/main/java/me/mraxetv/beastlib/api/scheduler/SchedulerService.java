package me.mraxetv.beastlib.api.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Cross-platform scheduler for Bukkit-family servers and Folia.
 *
 * <p>All delays and periods are expressed in server ticks. Global work must
 * only touch global state. Work involving an entity should use an entity or
 * player method, and work involving a block, chunk, or world position should
 * use a region/location method. Asynchronous callbacks must not access Bukkit
 * state unless that API explicitly documents asynchronous safety.</p>
 */
public interface SchedulerService {

    long TICK_MILLIS = 50L;

    boolean isFolia();

    boolean isShutdown();

    SchedulerTask runGlobal(Plugin owner, Runnable task);

    SchedulerTask runGlobalDelayed(Plugin owner, Runnable task, long delayTicks);

    SchedulerTask runGlobalRepeating(Plugin owner, Runnable task, long initialDelayTicks, long periodTicks);

    SchedulerTask runAsync(Plugin owner, Runnable task);

    SchedulerTask runAsyncDelayed(Plugin owner, Runnable task, long delayTicks);

    SchedulerTask runAsyncRepeating(Plugin owner, Runnable task, long initialDelayTicks, long periodTicks);

    default SchedulerTask runEntity(Plugin owner, Entity entity, Runnable task) {
        return runEntity(owner, entity, task, null);
    }

    SchedulerTask runEntity(Plugin owner, Entity entity, Runnable task, Runnable retired);

    default SchedulerTask runEntityDelayed(Plugin owner, Entity entity, Runnable task, long delayTicks) {
        return runEntityDelayed(owner, entity, task, delayTicks, null);
    }

    SchedulerTask runEntityDelayed(
            Plugin owner,
            Entity entity,
            Runnable task,
            long delayTicks,
            Runnable retired
    );

    default SchedulerTask runEntityRepeating(
            Plugin owner,
            Entity entity,
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    ) {
        return runEntityRepeating(owner, entity, task, initialDelayTicks, periodTicks, null);
    }

    SchedulerTask runEntityRepeating(
            Plugin owner,
            Entity entity,
            Runnable task,
            long initialDelayTicks,
            long periodTicks,
            Runnable retired
    );

    default SchedulerTask runPlayer(Plugin owner, Player player, Runnable task) {
        return runEntity(owner, player, task);
    }

    default SchedulerTask runPlayer(Plugin owner, Player player, Runnable task, Runnable retired) {
        return runEntity(owner, player, task, retired);
    }

    default SchedulerTask runPlayerDelayed(Plugin owner, Player player, Runnable task, long delayTicks) {
        return runEntityDelayed(owner, player, task, delayTicks);
    }

    default SchedulerTask runPlayerDelayed(
            Plugin owner,
            Player player,
            Runnable task,
            long delayTicks,
            Runnable retired
    ) {
        return runEntityDelayed(owner, player, task, delayTicks, retired);
    }

    default SchedulerTask runPlayerRepeating(
            Plugin owner,
            Player player,
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    ) {
        return runEntityRepeating(owner, player, task, initialDelayTicks, periodTicks);
    }

    default SchedulerTask runPlayerRepeating(
            Plugin owner,
            Player player,
            Runnable task,
            long initialDelayTicks,
            long periodTicks,
            Runnable retired
    ) {
        return runEntityRepeating(owner, player, task, initialDelayTicks, periodTicks, retired);
    }

    SchedulerTask runRegion(Plugin owner, Location location, Runnable task);

    SchedulerTask runRegionDelayed(Plugin owner, Location location, Runnable task, long delayTicks);

    SchedulerTask runRegionRepeating(
            Plugin owner,
            Location location,
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    );

    default SchedulerTask runAtLocation(Plugin owner, Location location, Runnable task) {
        return runRegion(owner, location, task);
    }

    default SchedulerTask runAtLocationDelayed(
            Plugin owner,
            Location location,
            Runnable task,
            long delayTicks
    ) {
        return runRegionDelayed(owner, location, task, delayTicks);
    }

    default SchedulerTask runAtLocationRepeating(
            Plugin owner,
            Location location,
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    ) {
        return runRegionRepeating(owner, location, task, initialDelayTicks, periodTicks);
    }

    /**
     * Cancels all unfinished tasks submitted through this service for an owner.
     * Tasks scheduled outside this service are not affected.
     *
     * @param owner plugin whose BeastLib tasks should be cancelled
     */
    void cancelTasks(Plugin owner);

    /**
     * Rejects new work and safely cancels all tracked work.
     */
    void shutdown();
}
