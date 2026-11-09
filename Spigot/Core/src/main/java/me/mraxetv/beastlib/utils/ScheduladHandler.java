package me.mraxetv.beastlib.utils;

import me.mraxetv.beastlib.BeastLib;
import me.mraxetv.beastlib.api.scheduler.SchedulerServices;
import me.mraxetv.beastlib.api.scheduler.SchedulerTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @deprecated The class name contains a historical spelling error. Use
 * {@link SchedulerHandler} or {@link me.mraxetv.beastlib.api.scheduler.SchedulerService}.
 */
@Deprecated
public final class ScheduladHandler {

    public static final long DEFAULT_DELAY_TICKS = 1200L;

    private ScheduladHandler() {
    }

    private static Plugin plugin() {
        Plugin plugin = BeastLib.getInstance();
        if (plugin == null) {
            throw new IllegalStateException("BeastLib instance is not available yet.");
        }
        return plugin;
    }

    public static BukkitTask runSync(Runnable runnable) {
        if (isFolia()) {
            return adapt(SchedulerHandler.runGlobal(runnable), true);
        }
        return Bukkit.getScheduler().runTask(plugin(), runnable);
    }

    public static BukkitTask runAsync(Runnable runnable) {
        if (isFolia()) {
            return adapt(SchedulerHandler.runAsync(runnable), false);
        }
        return Bukkit.getScheduler().runTaskAsynchronously(plugin(), runnable);
    }

    public static BukkitTask runSyncDelayed(Runnable runnable, long delay) {
        if (isFolia()) {
            return adapt(SchedulerHandler.runGlobalDelayed(runnable, delay), true);
        }
        return Bukkit.getScheduler().runTaskLater(plugin(), runnable, delay);
    }

    public static BukkitTask runSyncDelayed(Runnable runnable) {
        return runSyncDelayed(runnable, DEFAULT_DELAY_TICKS);
    }

    public static BukkitTask runAsyncDelayed(Runnable runnable, long delay) {
        if (isFolia()) {
            return adapt(SchedulerHandler.runAsyncDelayed(runnable, delay), false);
        }
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin(), runnable, delay);
    }

    public static BukkitTask runAsyncDelayed(Runnable runnable) {
        return runAsyncDelayed(runnable, DEFAULT_DELAY_TICKS);
    }

    public static BukkitTask runSyncScheduled(Runnable runnable, long firstRunDelay, long delay) {
        if (isFolia()) {
            return adapt(
                    SchedulerHandler.runGlobalRepeating(runnable, firstRunDelay, delay),
                    true
            );
        }
        return Bukkit.getScheduler().runTaskTimer(plugin(), runnable, firstRunDelay, delay);
    }

    public static BukkitTask runSyncScheduled(Runnable runnable, long delay) {
        return runSyncScheduled(runnable, DEFAULT_DELAY_TICKS, delay);
    }

    public static BukkitTask runSyncScheduled(Runnable runnable) {
        return runSyncScheduled(runnable, DEFAULT_DELAY_TICKS, DEFAULT_DELAY_TICKS);
    }

    public static BukkitTask runAsyncScheduled(Runnable runnable, long firstRunDelay, long delay) {
        if (isFolia()) {
            return adapt(
                    SchedulerHandler.runAsyncRepeating(runnable, firstRunDelay, delay),
                    false
            );
        }
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin(), runnable, firstRunDelay, delay);
    }

    public static BukkitTask runAsyncScheduled(Runnable runnable, long delay) {
        return runAsyncScheduled(runnable, DEFAULT_DELAY_TICKS, delay);
    }

    public static BukkitTask runAsyncScheduled(Runnable runnable) {
        return runAsyncScheduled(runnable, DEFAULT_DELAY_TICKS, DEFAULT_DELAY_TICKS);
    }

    private static boolean isFolia() {
        return SchedulerServices.isAvailable() && SchedulerServices.get().isFolia();
    }

    private static BukkitTask adapt(SchedulerTask task, boolean synchronous) {
        return new LegacyBukkitTask(task, synchronous);
    }

    private static final class LegacyBukkitTask implements BukkitTask {
        private static final AtomicInteger NEXT_ID = new AtomicInteger(-2);

        private final int taskId = NEXT_ID.getAndDecrement();
        private final SchedulerTask delegate;
        private final boolean synchronous;

        private LegacyBukkitTask(SchedulerTask delegate, boolean synchronous) {
            this.delegate = delegate;
            this.synchronous = synchronous;
        }

        @Override
        public int getTaskId() {
            return taskId;
        }

        @Override
        public Plugin getOwner() {
            return delegate.getOwner();
        }

        @Override
        public boolean isSync() {
            return synchronous;
        }

        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public void cancel() {
            delegate.cancel();
        }
    }

}
