package me.mraxetv.beastlib.scheduler;

import me.mraxetv.beastlib.api.scheduler.SchedulerService;
import me.mraxetv.beastlib.api.scheduler.SchedulerTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Bukkit implementation of {@link SchedulerService}. It uses Bukkit's task
 * scheduler on traditional servers and the Folia scheduler family through a
 * reflection-only bridge on Folia.
 */
public final class PlatformSchedulerService implements SchedulerService, Listener {

    private final Plugin lifecycleOwner;
    private final boolean folia;
    private final FoliaSchedulerBridge foliaBridge;
    private final Map<Plugin, Set<TrackedSchedulerTask>> tasks = new ConcurrentHashMap<>();

    private volatile boolean shutdown;
    private boolean listenerRegistered;

    public PlatformSchedulerService(Plugin lifecycleOwner) {
        if (lifecycleOwner == null) {
            throw new IllegalArgumentException("lifecycleOwner cannot be null");
        }
        this.lifecycleOwner = lifecycleOwner;
        this.folia = FoliaSchedulerBridge.isFolia(lifecycleOwner.getServer());
        this.foliaBridge = folia ? new FoliaSchedulerBridge(lifecycleOwner.getServer()) : null;
    }

    public synchronized void start() {
        if (shutdown) {
            throw new IllegalStateException("Scheduler service has already shut down.");
        }
        if (!listenerRegistered) {
            lifecycleOwner.getServer().getPluginManager().registerEvents(this, lifecycleOwner);
            listenerRegistered = true;
        }
    }

    @Override
    public boolean isFolia() {
        return folia;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public SchedulerTask runGlobal(Plugin owner, Runnable task) {
        final TrackedSchedulerTask handle = createTask(owner, task, false);
        try {
            if (folia) {
                bindFolia(handle, foliaBridge.runGlobal(owner, foliaConsumer(handle, task)));
            } else {
                bindBukkit(handle, Bukkit.getScheduler().runTask(owner, wrapped(handle, task)));
            }
            return handle;
        } catch (RuntimeException exception) {
            throw schedulingFailure(handle, "global task", exception);
        }
    }

    @Override
    public SchedulerTask runGlobalDelayed(Plugin owner, Runnable task, long delayTicks) {
        requireDelay(delayTicks, "delayTicks");
        if (delayTicks == 0L) {
            return runGlobal(owner, task);
        }

        final TrackedSchedulerTask handle = createTask(owner, task, false);
        try {
            if (folia) {
                bindFolia(
                        handle,
                        foliaBridge.runGlobalDelayed(owner, foliaConsumer(handle, task), delayTicks)
                );
            } else {
                bindBukkit(
                        handle,
                        Bukkit.getScheduler().runTaskLater(owner, wrapped(handle, task), delayTicks)
                );
            }
            return handle;
        } catch (RuntimeException exception) {
            throw schedulingFailure(handle, "delayed global task", exception);
        }
    }

    @Override
    public SchedulerTask runGlobalRepeating(
            Plugin owner,
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    ) {
        requireDelay(initialDelayTicks, "initialDelayTicks");
        requirePeriod(periodTicks);
        final TrackedSchedulerTask handle = createTask(owner, task, true);
        try {
            if (folia) {
                bindFolia(
                        handle,
                        foliaBridge.runGlobalRepeating(
                                owner,
                                foliaConsumer(handle, task),
                                foliaInitialDelay(initialDelayTicks),
                                periodTicks
                        )
                );
            } else {
                bindBukkit(
                        handle,
                        Bukkit.getScheduler().runTaskTimer(
                                owner,
                                wrapped(handle, task),
                                initialDelayTicks,
                                periodTicks
                        )
                );
            }
            return handle;
        } catch (RuntimeException exception) {
            throw schedulingFailure(handle, "repeating global task", exception);
        }
    }

    @Override
    public SchedulerTask runAsync(Plugin owner, Runnable task) {
        final TrackedSchedulerTask handle = createTask(owner, task, false);
        try {
            if (folia) {
                bindFolia(handle, foliaBridge.runAsync(owner, foliaConsumer(handle, task)));
            } else {
                bindBukkit(
                        handle,
                        Bukkit.getScheduler().runTaskAsynchronously(owner, wrapped(handle, task))
                );
            }
            return handle;
        } catch (RuntimeException exception) {
            throw schedulingFailure(handle, "asynchronous task", exception);
        }
    }

    @Override
    public SchedulerTask runAsyncDelayed(Plugin owner, Runnable task, long delayTicks) {
        requireDelay(delayTicks, "delayTicks");
        if (delayTicks == 0L) {
            return runAsync(owner, task);
        }

        final TrackedSchedulerTask handle = createTask(owner, task, false);
        try {
            if (folia) {
                bindFolia(
                        handle,
                        foliaBridge.runAsyncDelayed(
                                owner,
                                foliaConsumer(handle, task),
                                ticksToMillis(delayTicks)
                        )
                );
            } else {
                bindBukkit(
                        handle,
                        Bukkit.getScheduler().runTaskLaterAsynchronously(
                                owner,
                                wrapped(handle, task),
                                delayTicks
                        )
                );
            }
            return handle;
        } catch (RuntimeException exception) {
            throw schedulingFailure(handle, "delayed asynchronous task", exception);
        }
    }

    @Override
    public SchedulerTask runAsyncRepeating(
            Plugin owner,
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    ) {
        requireDelay(initialDelayTicks, "initialDelayTicks");
        requirePeriod(periodTicks);
        final TrackedSchedulerTask handle = createTask(owner, task, true);
        try {
            if (folia) {
                bindFolia(
                        handle,
                        foliaBridge.runAsyncRepeating(
                                owner,
                                foliaConsumer(handle, task),
                                ticksToMillis(foliaInitialDelay(initialDelayTicks)),
                                ticksToMillis(periodTicks)
                        )
                );
            } else {
                bindBukkit(
                        handle,
                        Bukkit.getScheduler().runTaskTimerAsynchronously(
                                owner,
                                wrapped(handle, task),
                                initialDelayTicks,
                                periodTicks
                        )
                );
            }
            return handle;
        } catch (RuntimeException exception) {
            throw schedulingFailure(handle, "repeating asynchronous task", exception);
        }
    }

    @Override
    public SchedulerTask runEntity(
            Plugin owner,
            Entity entity,
            Runnable task,
            Runnable retired
    ) {
        requireEntity(entity);
        final TrackedSchedulerTask handle = createTask(owner, task, false);
        try {
            if (folia) {
                Object nativeTask = foliaBridge.runEntity(
                        owner,
                        entity,
                        foliaConsumer(handle, task),
                        retiredCallback(handle, retired)
                );
                bindEntityResult(handle, nativeTask);
            } else {
                bindBukkit(handle, Bukkit.getScheduler().runTask(owner, wrapped(handle, task)));
            }
            return handle;
        } catch (RuntimeException exception) {
            throw schedulingFailure(handle, "entity task", exception);
        }
    }

    @Override
    public SchedulerTask runEntityDelayed(
            Plugin owner,
            Entity entity,
            Runnable task,
            long delayTicks,
            Runnable retired
    ) {
        requireEntity(entity);
        requireDelay(delayTicks, "delayTicks");
        if (delayTicks == 0L) {
            return runEntity(owner, entity, task, retired);
        }

        final TrackedSchedulerTask handle = createTask(owner, task, false);
        try {
            if (folia) {
                Object nativeTask = foliaBridge.runEntityDelayed(
                        owner,
                        entity,
                        foliaConsumer(handle, task),
                        retiredCallback(handle, retired),
                        delayTicks
                );
                bindEntityResult(handle, nativeTask);
            } else {
                bindBukkit(
                        handle,
                        Bukkit.getScheduler().runTaskLater(owner, wrapped(handle, task), delayTicks)
                );
            }
            return handle;
        } catch (RuntimeException exception) {
            throw schedulingFailure(handle, "delayed entity task", exception);
        }
    }

    @Override
    public SchedulerTask runEntityRepeating(
            Plugin owner,
            Entity entity,
            Runnable task,
            long initialDelayTicks,
            long periodTicks,
            Runnable retired
    ) {
        requireEntity(entity);
        requireDelay(initialDelayTicks, "initialDelayTicks");
        requirePeriod(periodTicks);
        final TrackedSchedulerTask handle = createTask(owner, task, true);
        try {
            if (folia) {
                Object nativeTask = foliaBridge.runEntityRepeating(
                        owner,
                        entity,
                        foliaConsumer(handle, task),
                        retiredCallback(handle, retired),
                        foliaInitialDelay(initialDelayTicks),
                        periodTicks
                );
                bindEntityResult(handle, nativeTask);
            } else {
                bindBukkit(
                        handle,
                        Bukkit.getScheduler().runTaskTimer(
                                owner,
                                wrapped(handle, task),
                                initialDelayTicks,
                                periodTicks
                        )
                );
            }
            return handle;
        } catch (RuntimeException exception) {
            throw schedulingFailure(handle, "repeating entity task", exception);
        }
    }

    @Override
    public SchedulerTask runRegion(Plugin owner, Location location, Runnable task) {
        final Location target = requireLocation(location);
        final TrackedSchedulerTask handle = createTask(owner, task, false);
        try {
            if (folia) {
                bindFolia(
                        handle,
                        foliaBridge.runRegion(owner, target, foliaConsumer(handle, task))
                );
            } else {
                bindBukkit(handle, Bukkit.getScheduler().runTask(owner, wrapped(handle, task)));
            }
            return handle;
        } catch (RuntimeException exception) {
            throw schedulingFailure(handle, "region task", exception);
        }
    }

    @Override
    public SchedulerTask runRegionDelayed(
            Plugin owner,
            Location location,
            Runnable task,
            long delayTicks
    ) {
        final Location target = requireLocation(location);
        requireDelay(delayTicks, "delayTicks");
        if (delayTicks == 0L) {
            return runRegion(owner, target, task);
        }

        final TrackedSchedulerTask handle = createTask(owner, task, false);
        try {
            if (folia) {
                bindFolia(
                        handle,
                        foliaBridge.runRegionDelayed(
                                owner,
                                target,
                                foliaConsumer(handle, task),
                                delayTicks
                        )
                );
            } else {
                bindBukkit(
                        handle,
                        Bukkit.getScheduler().runTaskLater(owner, wrapped(handle, task), delayTicks)
                );
            }
            return handle;
        } catch (RuntimeException exception) {
            throw schedulingFailure(handle, "delayed region task", exception);
        }
    }

    @Override
    public SchedulerTask runRegionRepeating(
            Plugin owner,
            Location location,
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    ) {
        final Location target = requireLocation(location);
        requireDelay(initialDelayTicks, "initialDelayTicks");
        requirePeriod(periodTicks);
        final TrackedSchedulerTask handle = createTask(owner, task, true);
        try {
            if (folia) {
                bindFolia(
                        handle,
                        foliaBridge.runRegionRepeating(
                                owner,
                                target,
                                foliaConsumer(handle, task),
                                foliaInitialDelay(initialDelayTicks),
                                periodTicks
                        )
                );
            } else {
                bindBukkit(
                        handle,
                        Bukkit.getScheduler().runTaskTimer(
                                owner,
                                wrapped(handle, task),
                                initialDelayTicks,
                                periodTicks
                        )
                );
            }
            return handle;
        } catch (RuntimeException exception) {
            throw schedulingFailure(handle, "repeating region task", exception);
        }
    }

    @Override
    public synchronized void cancelTasks(Plugin owner) {
        if (owner == null) {
            return;
        }
        Set<TrackedSchedulerTask> ownedTasks = tasks.remove(owner);
        if (ownedTasks == null) {
            return;
        }
        for (TrackedSchedulerTask task : ownedTasks.toArray(new TrackedSchedulerTask[0])) {
            task.cancel();
        }
        ownedTasks.clear();
    }

    @Override
    public synchronized void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;

        if (listenerRegistered) {
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
        }

        for (Plugin owner : tasks.keySet().toArray(new Plugin[0])) {
            cancelTasks(owner);
        }
        tasks.clear();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        cancelTasks(event.getPlugin());
    }

    private synchronized TrackedSchedulerTask createTask(
            Plugin owner,
            Runnable task,
            boolean repeating
    ) {
        if (owner == null) {
            throw new IllegalArgumentException("owner cannot be null");
        }
        if (task == null) {
            throw new IllegalArgumentException("task cannot be null");
        }
        if (!owner.isEnabled()) {
            throw new IllegalStateException("Task owner " + owner.getName() + " is not enabled.");
        }
        if (shutdown) {
            throw new IllegalStateException("BeastLib scheduler has shut down.");
        }

        Set<TrackedSchedulerTask> ownedTasks = tasks.get(owner);
        if (ownedTasks == null) {
            Set<TrackedSchedulerTask> created = Collections.newSetFromMap(
                    new ConcurrentHashMap<TrackedSchedulerTask, Boolean>()
            );
            Set<TrackedSchedulerTask> existing = tasks.putIfAbsent(owner, created);
            ownedTasks = existing == null ? created : existing;
        }

        final Set<TrackedSchedulerTask> taskSet = ownedTasks;
        TrackedSchedulerTask handle = new TrackedSchedulerTask(
                owner,
                repeating,
                new TrackedSchedulerTask.TerminalListener() {
                    @Override
                    public void onTerminal(TrackedSchedulerTask terminalTask) {
                        taskSet.remove(terminalTask);
                    }
                }
        );
        ownedTasks.add(handle);

        if (shutdown) {
            handle.cancel();
            throw new IllegalStateException("BeastLib scheduler has shut down.");
        }
        return handle;
    }

    private Consumer<Object> foliaConsumer(
            final TrackedSchedulerTask handle,
            final Runnable task
    ) {
        return new Consumer<Object>() {
            @Override
            public void accept(Object nativeTask) {
                bindFolia(handle, nativeTask);
                handle.execute(task);
            }
        };
    }

    private Runnable retiredCallback(
            final TrackedSchedulerTask handle,
            final Runnable callback
    ) {
        return new Runnable() {
            @Override
            public void run() {
                if (!handle.retire() || callback == null) {
                    return;
                }
                try {
                    callback.run();
                } catch (Throwable throwable) {
                    handle.getOwner().getLogger().log(
                            Level.SEVERE,
                            "Entity scheduler retired callback failed.",
                            throwable
                    );
                }
            }
        };
    }

    private void bindEntityResult(TrackedSchedulerTask handle, Object nativeTask) {
        if (nativeTask == null) {
            handle.retire();
        } else {
            bindFolia(handle, nativeTask);
        }
    }

    private void bindFolia(TrackedSchedulerTask handle, Object nativeTask) {
        handle.bindCancellation(foliaBridge.cancellation(nativeTask));
    }

    private static void bindBukkit(TrackedSchedulerTask handle, final BukkitTask nativeTask) {
        handle.bindCancellation(new Runnable() {
            @Override
            public void run() {
                nativeTask.cancel();
            }
        });
    }

    private static Runnable wrapped(
            final TrackedSchedulerTask handle,
            final Runnable task
    ) {
        return new Runnable() {
            @Override
            public void run() {
                handle.execute(task);
            }
        };
    }

    private static IllegalStateException schedulingFailure(
            TrackedSchedulerTask handle,
            String description,
            RuntimeException cause
    ) {
        handle.reject();
        return new IllegalStateException("Could not schedule " + description + '.', cause);
    }

    private static void requireEntity(Entity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity cannot be null");
        }
    }

    private static Location requireLocation(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("location cannot be null");
        }
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("location must have a world");
        }
        return location.clone();
    }

    private static void requireDelay(long delayTicks, String name) {
        if (delayTicks < 0L) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
    }

    private static void requirePeriod(long periodTicks) {
        if (periodTicks <= 0L) {
            throw new IllegalArgumentException("periodTicks must be greater than zero");
        }
    }

    private static long foliaInitialDelay(long initialDelayTicks) {
        return initialDelayTicks == 0L ? 1L : initialDelayTicks;
    }

    private static long ticksToMillis(long ticks) {
        if (ticks > Long.MAX_VALUE / TICK_MILLIS) {
            return Long.MAX_VALUE;
        }
        return ticks * TICK_MILLIS;
    }
}
