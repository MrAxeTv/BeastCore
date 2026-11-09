# Platform scheduler

BeastLib exposes a Java 8 scheduler API from
`me.mraxetv.beastlib.api.scheduler`. It uses the Bukkit scheduler on
CraftBukkit, Spigot, Paper, and compatible forks. On Folia it calls the
global, entity, region, and asynchronous schedulers through reflection, so
plugins do not need a compile-time Paper or Folia dependency.

## Access

Plugins which implement `BeastLibAPI` can call `getScheduler()`. Other plugins
can use `SchedulerServices.get()` or Bukkit's `ServicesManager`:

```java
SchedulerService scheduler = SchedulerServices.get();
```

Always pass the plugin which owns the task. BeastLib tracks ownership and
cancels the owner's unfinished tasks when that plugin disables:

```java
scheduler.runGlobal(plugin, () -> reloadSharedState());
scheduler.runAsync(plugin, () -> saveToDatabase());
scheduler.runEntity(plugin, player, () -> updatePlayer(player));
scheduler.runRegion(plugin, block.getLocation(), () -> updateBlock(block));
```

Delayed and repeating forms use ticks:

```java
SchedulerTask task = scheduler.runEntityRepeating(
        plugin,
        player,
        () -> updatePlayerMenu(player),
        1L,
        20L,
        () -> closePlayerSession(player.getUniqueId())
);
```

Use entity/player scheduling for any entity work. Folia's entity scheduler
follows an entity when it changes regions. Use region/location scheduling for
blocks, chunks, or other position-owned state. Global callbacks are only for
global server state. Async callbacks must not access Bukkit state unless the
specific Bukkit API documents that access as thread-safe.

## Task lifecycle

Every submission returns `SchedulerTask`. Its state is one of `SCHEDULED`,
`RUNNING`, `COMPLETED`, `CANCELLED`, `RETIRED`, or `REJECTED`. Calling
`cancel()` is idempotent and never interrupts a callback which is already
running. It prevents future executions of repeating work.

An entity task becomes `RETIRED` if Folia removes the entity before the task
can run. The optional retired callback is invoked by Folia only for a task
which was accepted and retired later. If Folia rejects the submission because
the entity was already removed, BeastLib returns a handle in `RETIRED` state
and does not run the callback on the caller's potentially unsafe thread.

Zero is accepted for one-shot delays and routes to the immediate form. Folia
requires repeating tasks to start at least one tick later, so a repeating
initial delay of zero is normalized to one tick on Folia. Async Folia delays
are converted at 50 milliseconds per configured tick.

`cancelTasks(plugin)` only cancels tasks submitted through this BeastLib
service. It does not cancel unrelated native tasks owned by that plugin.

## Legacy compatibility

`me.mraxetv.beastlib.utils.ScheduladHandler` remains binary compatible and is
deprecated. On Bukkit it still returns native `BukkitTask` objects. On Folia it
returns a compatibility `BukkitTask` adapter backed by `SchedulerTask`; its
negative synthetic task ID cannot be queried through Bukkit's deprecated
numeric task-ID methods. New code should use `SchedulerService`, or the
correctly named `SchedulerHandler` only for BeastLib-owned convenience tasks.
