package me.mraxetv.beastlib.scheduler;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Reflective bridge which keeps Folia and Paper scheduler classes out of the
 * BeastLib compile-time API and bytecode signatures.
 */
final class FoliaSchedulerBridge {

    private static final String FOLIA_SERVER_CLASS =
            "io.papermc.paper.threadedregions.RegionizedServer";
    private static final String SCHEDULER_PACKAGE =
            "io.papermc.paper.threadedregions.scheduler.";

    private final Object globalScheduler;
    private final Object regionScheduler;
    private final Object asyncScheduler;

    private final Method globalRun;
    private final Method globalRunDelayed;
    private final Method globalRunRepeating;
    private final Method regionRun;
    private final Method regionRunDelayed;
    private final Method regionRunRepeating;
    private final Method asyncRun;
    private final Method asyncRunDelayed;
    private final Method asyncRunRepeating;
    private final Method entityGetScheduler;
    private final Method entityRun;
    private final Method entityRunDelayed;
    private final Method entityRunRepeating;
    private final Method scheduledTaskCancel;

    static boolean isFolia(Server server) {
        if (server == null) {
            return false;
        }
        try {
            Class.forName(FOLIA_SERVER_CLASS, false, server.getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (LinkageError ignored) {
            return false;
        } catch (SecurityException ignored) {
            return false;
        }
    }

    FoliaSchedulerBridge(Server server) {
        try {
            ClassLoader loader = server.getClass().getClassLoader();
            Class<?> globalType = Class.forName(
                    SCHEDULER_PACKAGE + "GlobalRegionScheduler", false, loader
            );
            Class<?> regionType = Class.forName(
                    SCHEDULER_PACKAGE + "RegionScheduler", false, loader
            );
            Class<?> asyncType = Class.forName(
                    SCHEDULER_PACKAGE + "AsyncScheduler", false, loader
            );
            Class<?> entityType = Class.forName(
                    SCHEDULER_PACKAGE + "EntityScheduler", false, loader
            );
            Class<?> scheduledTaskType = Class.forName(
                    SCHEDULER_PACKAGE + "ScheduledTask", false, loader
            );

            Method getGlobal = Server.class.getMethod("getGlobalRegionScheduler");
            Method getRegion = Server.class.getMethod("getRegionScheduler");
            Method getAsync = Server.class.getMethod("getAsyncScheduler");

            globalScheduler = invoke(getGlobal, server);
            regionScheduler = invoke(getRegion, server);
            asyncScheduler = invoke(getAsync, server);

            globalRun = globalType.getMethod("run", Plugin.class, Consumer.class);
            globalRunDelayed = globalType.getMethod(
                    "runDelayed", Plugin.class, Consumer.class, Long.TYPE
            );
            globalRunRepeating = globalType.getMethod(
                    "runAtFixedRate", Plugin.class, Consumer.class, Long.TYPE, Long.TYPE
            );

            regionRun = regionType.getMethod(
                    "run", Plugin.class, Location.class, Consumer.class
            );
            regionRunDelayed = regionType.getMethod(
                    "runDelayed", Plugin.class, Location.class, Consumer.class, Long.TYPE
            );
            regionRunRepeating = regionType.getMethod(
                    "runAtFixedRate",
                    Plugin.class,
                    Location.class,
                    Consumer.class,
                    Long.TYPE,
                    Long.TYPE
            );

            asyncRun = asyncType.getMethod("runNow", Plugin.class, Consumer.class);
            asyncRunDelayed = asyncType.getMethod(
                    "runDelayed", Plugin.class, Consumer.class, Long.TYPE, TimeUnit.class
            );
            asyncRunRepeating = asyncType.getMethod(
                    "runAtFixedRate",
                    Plugin.class,
                    Consumer.class,
                    Long.TYPE,
                    Long.TYPE,
                    TimeUnit.class
            );

            entityGetScheduler = Entity.class.getMethod("getScheduler");
            entityRun = entityType.getMethod(
                    "run", Plugin.class, Consumer.class, Runnable.class
            );
            entityRunDelayed = entityType.getMethod(
                    "runDelayed", Plugin.class, Consumer.class, Runnable.class, Long.TYPE
            );
            entityRunRepeating = entityType.getMethod(
                    "runAtFixedRate",
                    Plugin.class,
                    Consumer.class,
                    Runnable.class,
                    Long.TYPE,
                    Long.TYPE
            );
            scheduledTaskCancel = scheduledTaskType.getMethod("cancel");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unsupported Folia scheduler API", exception);
        }
    }

    Object runGlobal(Plugin owner, Consumer<Object> task) {
        return invoke(globalRun, globalScheduler, owner, task);
    }

    Object runGlobalDelayed(Plugin owner, Consumer<Object> task, long delayTicks) {
        return invoke(globalRunDelayed, globalScheduler, owner, task, delayTicks);
    }

    Object runGlobalRepeating(
            Plugin owner,
            Consumer<Object> task,
            long initialDelayTicks,
            long periodTicks
    ) {
        return invoke(
                globalRunRepeating,
                globalScheduler,
                owner,
                task,
                initialDelayTicks,
                periodTicks
        );
    }

    Object runRegion(Plugin owner, Location location, Consumer<Object> task) {
        return invoke(regionRun, regionScheduler, owner, location, task);
    }

    Object runRegionDelayed(
            Plugin owner,
            Location location,
            Consumer<Object> task,
            long delayTicks
    ) {
        return invoke(regionRunDelayed, regionScheduler, owner, location, task, delayTicks);
    }

    Object runRegionRepeating(
            Plugin owner,
            Location location,
            Consumer<Object> task,
            long initialDelayTicks,
            long periodTicks
    ) {
        return invoke(
                regionRunRepeating,
                regionScheduler,
                owner,
                location,
                task,
                initialDelayTicks,
                periodTicks
        );
    }

    Object runAsync(Plugin owner, Consumer<Object> task) {
        return invoke(asyncRun, asyncScheduler, owner, task);
    }

    Object runAsyncDelayed(Plugin owner, Consumer<Object> task, long delayMillis) {
        return invoke(
                asyncRunDelayed,
                asyncScheduler,
                owner,
                task,
                delayMillis,
                TimeUnit.MILLISECONDS
        );
    }

    Object runAsyncRepeating(
            Plugin owner,
            Consumer<Object> task,
            long initialDelayMillis,
            long periodMillis
    ) {
        return invoke(
                asyncRunRepeating,
                asyncScheduler,
                owner,
                task,
                initialDelayMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
        );
    }

    Object runEntity(Plugin owner, Entity entity, Consumer<Object> task, Runnable retired) {
        Object scheduler = invoke(entityGetScheduler, entity);
        return invoke(entityRun, scheduler, owner, task, retired);
    }

    Object runEntityDelayed(
            Plugin owner,
            Entity entity,
            Consumer<Object> task,
            Runnable retired,
            long delayTicks
    ) {
        Object scheduler = invoke(entityGetScheduler, entity);
        return invoke(entityRunDelayed, scheduler, owner, task, retired, delayTicks);
    }

    Object runEntityRepeating(
            Plugin owner,
            Entity entity,
            Consumer<Object> task,
            Runnable retired,
            long initialDelayTicks,
            long periodTicks
    ) {
        Object scheduler = invoke(entityGetScheduler, entity);
        return invoke(
                entityRunRepeating,
                scheduler,
                owner,
                task,
                retired,
                initialDelayTicks,
                periodTicks
        );
    }

    Runnable cancellation(final Object scheduledTask) {
        if (scheduledTask == null) {
            return null;
        }
        return new Runnable() {
            @Override
            public void run() {
                invoke(scheduledTaskCancel, scheduledTask);
            }
        };
    }

    private static Object invoke(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access Folia scheduler method " + method.getName(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Folia scheduler method failed: " + method.getName(), cause);
        }
    }
}
