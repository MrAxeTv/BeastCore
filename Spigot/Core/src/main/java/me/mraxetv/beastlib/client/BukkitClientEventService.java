package me.mraxetv.beastlib.client;

import me.mraxetv.beastlib.api.client.ClientChunkSnapshot;
import me.mraxetv.beastlib.api.client.ClientEventRegistration;
import me.mraxetv.beastlib.api.client.ClientEventService;
import me.mraxetv.beastlib.api.event.player.PlayerChunkSentEvent;
import me.mraxetv.beastlib.api.event.player.PlayerClientTickEndEvent;
import me.mraxetv.beastlib.api.event.player.PlayerUnknownEntityUseEvent;
import me.mraxetv.beastlib.api.scheduler.SchedulerServices;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Neutral client observations. Paper forwards its real synchronous events;
 * missing events use a connection-owned, reflection-only packet observer.
 * No modern native classes are resolved until an enabled consumer watches.
 */
public final class BukkitClientEventService implements ClientEventService, Listener {
    private static final int MAX_PENDING = 2048;
    private static final int MAX_SENT_CHUNKS = 262144;
    private final Plugin lifecycleOwner;
    private final CopyOnWriteArrayList<Registration> registrations = new CopyOnWriteArrayList<Registration>();
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    private final AtomicLong generations = new AtomicLong();
    private final Listener paperListener = new Listener() { };
    private volatile Map<String, UUID> worldIds = Collections.emptyMap();
    private volatile boolean shutdown;
    private volatile long inputEpoch;
    private boolean started;
    private NativeClientBinding binding;
    private boolean paperChunks, paperTicks, paperUnknown;
    private boolean adaptersReady;

    public BukkitClientEventService(Plugin lifecycleOwner) {
        this.lifecycleOwner = Objects.requireNonNull(lifecycleOwner, "lifecycleOwner");
    }

    public void start() {
        requireLifecycleThread();
        if (shutdown) throw new IllegalStateException("Client event service has shut down");
        if (!started) {
            lifecycleOwner.getServer().getPluginManager().registerEvents(this, lifecycleOwner);
            started = true;
        }
    }

    @Override
    public ClientEventRegistration watch(Plugin owner, Consumer<Player> failureCallback) {
        requireServerThread();
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(failureCallback, "failureCallback");
        if (shutdown || !started) throw new IllegalStateException("Client event service is not running");
        if (!owner.isEnabled()) throw new IllegalArgumentException("Client event owner must be enabled");
        if (SchedulerServices.get().isFolia())
            throw new IllegalStateException("Native client event observation has not been validated on Folia");
        prepareAdapters();
        if (registrations.isEmpty()) inputEpoch++;
        Registration registration = new Registration(owner, failureCallback);
        registrations.add(registration);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Session prior = sessions.get(player.getUniqueId());
            attach(player);
            Session session = sessions.get(player.getUniqueId());
            if (session != null && session == prior && session.player == player && session.failed.get()) {
                try { failureCallback.accept(player); }
                catch (RuntimeException error) {
                    lifecycleOwner.getLogger().log(Level.WARNING, "Client stream failure callback failed for " + owner.getName(), error);
                }
            }
        }
        return registration;
    }

    @Override
    public ClientChunkSnapshot sentChunks(Player player) {
        requireServerThread();
        Objects.requireNonNull(player, "player");
        UUID world = player.getWorld().getUID();
        Session session = sessions.get(player.getUniqueId());
        if (session == null || session.player != player || session.failed.get()
                || !currentConnection(session) || !world.equals(session.sentWorld))
            return new ClientChunkSnapshot(world, Collections.<Long>emptySet());
        if (paperChunks) {
            // Paper may not have initialized its chunk loader during PlayerJoinEvent.
            // Until this optional API is ready, retain only actual Paper send receipts.
            try {
                Set<Long> keys = paperSentChunks(session);
                if (keys != null) {
                    session.sentChunks.clear();
                    session.sentChunks.addAll(keys);
                }
            } catch (RuntimeException error) {
                fail(session, error);
                return new ClientChunkSnapshot(world, Collections.<Long>emptySet());
            }
        }
        return new ClientChunkSnapshot(world, session.sentChunks);
    }

    @Override
    public void close(Plugin owner) {
        if (owner == null) return;
        for (Registration registration : registrations)
            if (registration.owner == owner) registration.close();
    }

    @Override public boolean isShutdown() { return shutdown; }

    public void shutdown() {
        requireLifecycleThread();
        if (shutdown) return;
        shutdown = true;
        for (Registration registration : registrations) registration.closed.set(true);
        registrations.clear();
        for (Session session : new ArrayList<Session>(sessions.values())) detach(session);
        sessions.clear();
        HandlerList.unregisterAll(this);
        HandlerList.unregisterAll(paperListener);
        started = false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (adaptersReady) attach(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null && session.player == event.getPlayer()) {
            sessions.remove(event.getPlayer().getUniqueId());
            detach(session);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null && session.player == event.getPlayer()) {
            session.sentChunks.clear();
            session.sentWorld = event.getPlayer().getWorld().getUID();
            // Do not overwrite wireWorld here: outbound Respawn ordering owns it.
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (binding != null) bindWorlds();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        UUID removed = event.getWorld().getUID();
        Map<String, UUID> remaining = new HashMap<String, UUID>();
        for (Map.Entry<String, UUID> entry : worldIds.entrySet())
            if (!removed.equals(entry.getValue())) remaining.put(entry.getKey(), entry.getValue());
        worldIds = Collections.unmodifiableMap(remaining);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) { close(event.getPlugin()); }

    private void prepareAdapters() {
        if (adaptersReady) return;
        try {
            Class<? extends Event> chunkLoad = paperEvent("io.papermc.paper.event.packet.PlayerChunkLoadEvent");
            Class<? extends Event> chunkUnload = paperEvent("io.papermc.paper.event.packet.PlayerChunkUnloadEvent");
            paperChunks = chunkLoad != null && chunkUnload != null;
            if (paperChunks) {
                registerPaper(chunkLoad, this::paperChunk);
                registerPaper(chunkUnload, this::paperChunkUnload);
            }
            paperTicks = registerPaper(paperEvent("io.papermc.paper.event.packet.ClientTickEndEvent"), this::paperTick);
            paperUnknown = registerPaper(paperEvent("com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent"), this::paperUnknown);
            if (!paperChunks || !paperTicks || !paperUnknown) {
                binding = new NativeClientBinding(lifecycleOwner.getClass().getClassLoader());
                bindWorlds();
            }
            adaptersReady = true;
        } catch (ReflectiveOperationException | LinkageError error) {
            resetAdapters();
            throw new IllegalStateException("Native client event observation is unavailable on this server", cause(error));
        } catch (RuntimeException error) {
            resetAdapters();
            throw error;
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Event> paperEvent(String name) {
        Class<?> type;
        try { type = Class.forName(name, false, lifecycleOwner.getClass().getClassLoader()); }
        catch (ClassNotFoundException absent) { return null; }
        if (!Event.class.isAssignableFrom(type)) throw new IllegalStateException(name + " is not a Bukkit event");
        return (Class<? extends Event>) type;
    }

    private boolean registerPaper(Class<? extends Event> type, final PaperCallback callback) {
        if (type == null) return false;
        lifecycleOwner.getServer().getPluginManager().registerEvent(type, paperListener,
                EventPriority.HIGHEST, new EventExecutor() {
                    @Override public void execute(Listener ignored, Event event) throws EventException {
                        if (shutdown) return;
                        try { callback.accept(event); }
                        catch (ReflectiveOperationException error) { throw new EventException(cause(error)); }
                    }
                }, lifecycleOwner, false);
        return true;
    }

    private void resetAdapters() {
        HandlerList.unregisterAll(paperListener);
        binding = null;
        worldIds = Collections.emptyMap();
        paperChunks = paperTicks = paperUnknown = false;
    }

    private void bindWorlds() {
        Map<String, UUID> result = new HashMap<String, UUID>();
        for (World world : Bukkit.getWorlds()) {
            try {
                String key = binding.worldKey(world);
                UUID prior = result.put(key, world.getUID());
                if (prior != null && !prior.equals(world.getUID()))
                    throw new IllegalStateException("Duplicate native world key " + key);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Could not bind world identity for client observations", cause(error));
            }
        }
        worldIds = Collections.unmodifiableMap(result);
    }

    private void attach(Player player) {
        if (shutdown || !player.isOnline()) return;
        Session prior = sessions.get(player.getUniqueId());
        if (prior != null && prior.player == player) return;
        if (prior != null) detach(prior);
        try {
            Session session = new Session(player, binding == null ? null : binding.connection(player), generations.incrementAndGet());
            sessions.put(player.getUniqueId(), session);
            if (binding != null) {
                session.dispatcher = new ClientEventDispatcher(MAX_PENDING, task -> {
                    try { binding.executeOnServer(task); }
                    catch (ReflectiveOperationException error) { throw new IllegalStateException("Native packet executor unavailable", cause(error)); }
                }, () -> !shutdown && !session.closed.get() && !session.failed.get(), error -> fail(session, error));
                installObserver(session);
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            Session session = sessions.get(player.getUniqueId());
            if (session != null && session.player == player) fail(session, error);
            else notifyOwners(player, error);
        }
    }

    private Set<Long> paperSentChunks(Session session) {
        Object value;
        try {
            Method method = session.player.getClass().getMethod("getSentChunkKeys");
            value = method.invoke(session.player);
        } catch (NoSuchMethodException absent) {
            return null;
        } catch (ReflectiveOperationException error) {
            Throwable failure = cause(error);
            if (failure instanceof VirtualMachineError) throw (VirtualMachineError) failure;
            if (failure instanceof ThreadDeath) throw (ThreadDeath) failure;
            // A snapshot is supplementary: a join-time getter failure must not
            // disable Paper input events or require the player to reconnect.
            if (!session.snapshotFailureLogged) {
                session.snapshotFailureLogged = true;
                lifecycleOwner.getLogger().log(Level.FINE,
                        "Paper sent chunk snapshot unavailable; retaining observed receipts for "
                                + session.player.getUniqueId(), cause(error));
            }
            return null;
        }
        if (value == null) return null;
        if (!(value instanceof Iterable<?>)) throw new IllegalStateException("Invalid Paper sent chunk snapshot");
        Set<Long> keys = new LinkedHashSet<Long>();
        for (Object key : (Iterable<?>) value) {
            if (!(key instanceof Long)) throw new IllegalStateException("Non-long sent chunk key");
            keys.add((Long) key);
            if (keys.size() > MAX_SENT_CHUNKS) throw new IllegalStateException("Sent chunk receipt budget exceeded");
        }
        return keys;
    }

    private void paperChunk(Event event) throws ReflectiveOperationException {
        Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
        Session session = session(player);
        if (session == null) return;
        Chunk chunk = (Chunk) event.getClass().getMethod("getChunk").invoke(event);
        UUID world = chunk.getWorld().getUID();
        if (!world.equals(player.getWorld().getUID())) return;
        recordChunk(session, world, chunk.getX(), chunk.getZ());
    }

    private void paperChunkUnload(Event event) throws ReflectiveOperationException {
        Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
        Session session = session(player);
        if (session == null) return;
        Chunk chunk = (Chunk) event.getClass().getMethod("getChunk").invoke(event);
        if (chunk.getWorld().getUID().equals(session.sentWorld))
            session.sentChunks.remove(ClientChunkSnapshot.chunkKey(chunk.getX(), chunk.getZ()));
    }

    private void paperTick(Event event) throws ReflectiveOperationException {
        if (registrations.isEmpty()) return;
        Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
        if (session(player) != null) publish(new PlayerClientTickEndEvent(player));
    }

    private void paperUnknown(Event event) throws ReflectiveOperationException {
        if (registrations.isEmpty()) return;
        Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
        if (session(player) == null) return;
        int entityId = ((Number) event.getClass().getMethod("getEntityId").invoke(event)).intValue();
        boolean attack = Boolean.TRUE.equals(event.getClass().getMethod("isAttack").invoke(event));
        EquipmentSlot hand = (EquipmentSlot) event.getClass().getMethod("getHand").invoke(event);
        Vector clicked = (Vector) event.getClass().getMethod("getClickedRelativePosition").invoke(event);
        publish(new PlayerUnknownEntityUseEvent(player, entityId, attack, hand, clicked));
    }

    private Session session(Player player) {
        Session session = sessions.get(player.getUniqueId());
        return session != null && session.player == player && !session.failed.get() && currentConnection(session) ? session : null;
    }

    private boolean currentConnection(Session session) {
        if (shutdown || session.closed.get() || sessions.get(session.player.getUniqueId()) != session
                || !session.player.isOnline()) return false;
        if (binding == null) return true;
        try { return binding.matches(session.player, session.connection); }
        catch (ReflectiveOperationException | RuntimeException error) { return false; }
    }

    private void recordChunk(Session session, UUID world, int x, int z) {
        if (!world.equals(session.sentWorld)) {
            session.sentWorld = world;
            session.sentChunks.clear();
        }
        long key = ClientChunkSnapshot.chunkKey(x, z);
        if (!session.sentChunks.contains(key) && session.sentChunks.size() >= MAX_SENT_CHUNKS) {
            fail(session, new IllegalStateException("Sent chunk receipt budget exceeded"));
            return;
        }
        session.sentChunks.add(key);
        publish(new PlayerChunkSentEvent(session.player, world, x, z));
    }

    private void publish(Event event) {
        if (!shutdown && !registrations.isEmpty()) lifecycleOwner.getServer().getPluginManager().callEvent(event);
    }

    private void installObserver(final Session session) throws ReflectiveOperationException {
        session.handler = Proxy.newProxyInstance(binding.inbound.getClassLoader(),
                new Class<?>[]{binding.inbound, binding.outbound},
                (proxy, method, args) -> packetHandler(session, proxy, method, args));
        binding.execute(session.connection.channel, () -> {
            if (session.closed.get() || session.failed.get()) return;
            try {
                Object pipeline = binding.pipeline.invoke(session.connection.channel);
                binding.addBefore.invoke(pipeline, "packet_handler", session.handlerName, session.handler);
            } catch (Throwable error) { fail(session, error); }
        });
    }

    private Object packetHandler(final Session session, Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if (method.getDeclaringClass() == Object.class) {
            if ("toString".equals(name)) return session.handlerName;
            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
            if ("equals".equals(name)) return proxy == args[0];
            return null;
        }
        if ("handlerAdded".equals(name) || "handlerRemoved".equals(name)) return null;
        Object context = args[0];
        if ("channelRead".equals(name)) {
            Object packet = args[1];
            // All downstream interceptors append their frame markers, and the
            // native handler queues its movement work, before our event task.
            binding.fireRead.invoke(context, packet);
            if (!registrations.isEmpty() && !session.closed.get() && !session.failed.get()) {
                try {
                    if (!paperTicks && binding.tickPacket.isInstance(packet))
                        enqueue(session, Observation.tick(session, inputEpoch));
                    else if (!paperUnknown) {
                        NativeClientBinding.UnknownUse use = binding.unknownUse(packet);
                        if (use != null) enqueue(session, Observation.use(session, inputEpoch, use.entity, use.attack, use.hand, use.clicked));
                    }
                } catch (Throwable error) { fail(session, error); }
            }
            return null;
        }
        if ("write".equals(name)) {
            Object packet = args[1];
            Object promise = args[2];
            if (!session.closed.get() && !session.failed.get()) {
                try {
                    final List<Observation> observations = new ArrayList<Observation>();
                    collectOutbound(session, packet, observations, 0);
                    if (!observations.isEmpty()) {
                        promise = binding.unvoid.invoke(promise);
                        Object listener = Proxy.newProxyInstance(binding.futureListener.getClassLoader(),
                                new Class<?>[]{binding.futureListener}, (object, callback, values) -> {
                                    if ("operationComplete".equals(callback.getName())) {
                                        try {
                                            if (Boolean.TRUE.equals(binding.futureSuccess.invoke(values[0])))
                                                for (Observation observation : observations) enqueue(session, observation);
                                        } catch (Throwable error) { fail(session, error); }
                                    } else if ("toString".equals(callback.getName())) return session.handlerName + "_send";
                                    else if ("hashCode".equals(callback.getName())) return System.identityHashCode(object);
                                    else if ("equals".equals(callback.getName())) return object == values[0];
                                    return null;
                                });
                        binding.addFutureListener.invoke(promise, listener);
                    }
                } catch (Throwable error) { fail(session, error); }
            }
            binding.write.invoke(context, packet, promise);
            return null;
        }
        if ("channelInactive".equals(name)) session.closed.set(true);
        Method forward = binding.forwards.get(name);
        if (forward != null) return forward.invoke(context, Arrays.copyOfRange(args, 1, args.length));
        return null;
    }

    private void collectOutbound(Session session, Object packet, List<Observation> result, int depth)
            throws ReflectiveOperationException {
        if (depth > 4 || result.size() > MAX_PENDING) throw new IllegalStateException("Client packet bundle budget exceeded");
        if (binding.bundlePacket.isInstance(packet)) {
            for (Object child : (Iterable<?>) binding.bundlePackets.invoke(packet)) collectOutbound(session, child, result, depth + 1);
        } else if (binding.loginPacket.isInstance(packet) || binding.respawnPacket.isInstance(packet)) {
            Object info = binding.loginPacket.isInstance(packet) ? binding.loginInfo.invoke(packet) : binding.respawnInfo.invoke(packet);
            String key = binding.dimensionKey(binding.spawnDimension.invoke(info));
            UUID world = worldIds.get(key);
            if (world == null) throw new IllegalStateException("Unbound native client world " + key);
            session.wireWorld = world;
            session.wireEpoch++;
            result.add(Observation.reset(session));
        } else if (!paperChunks && binding.chunkPacket.isInstance(packet)) {
            result.add(Observation.chunk(session, ((Number) binding.chunkX.invoke(packet)).intValue(),
                    ((Number) binding.chunkZ.invoke(packet)).intValue(), false));
        } else if (!paperChunks && binding.forgetPacket.isInstance(packet)) {
            Object position = binding.forgetPosition.invoke(packet);
            result.add(Observation.chunk(session, binding.positionX.getInt(position), binding.positionZ.getInt(position), true));
        }
    }

    private void enqueue(final Session session, final Observation observation) {
        // One native FIFO task per event. A shared drain could pull a later
        // frame ahead of native movement tasks queued between boundaries.
        session.dispatcher.submit(() -> {
            if (!Bukkit.isPrimaryThread()) {
                fail(session, new IllegalStateException("Native client callback was not on the server thread"));
                return;
            }
            if (!currentConnection(session) || observation.epoch != session.wireEpoch
                    || !observation.world.equals(session.player.getWorld().getUID())) return;
            try { dispatch(session, observation); }
            catch (ReflectiveOperationException | RuntimeException error) { fail(session, error); }
        });
    }

    private void dispatch(Session session, Observation observation) throws ReflectiveOperationException {
        // Receipt observation outlives consumers; input queued for a completely
        // closed owner generation must not enter a newly opened controller.
        if ((observation.kind == Observation.TICK || observation.kind == Observation.USE)
                && observation.watchEpoch != inputEpoch) return;
        if (observation.kind == Observation.TICK) publish(new PlayerClientTickEndEvent(session.player));
        else if (observation.kind == Observation.USE) {
            if (!binding.entityExists(session.player.getWorld(), observation.x))
                publish(new PlayerUnknownEntityUseEvent(session.player, observation.x, observation.attack,
                        observation.hand, observation.clicked));
        } else if (observation.kind == Observation.RESET) {
            session.sentChunks.clear(); session.sentWorld = observation.world;
        } else if (observation.kind == Observation.FORGET) {
            if (observation.world.equals(session.sentWorld))
                session.sentChunks.remove(ClientChunkSnapshot.chunkKey(observation.x, observation.z));
        } else recordChunk(session, observation.world, observation.x, observation.z);
    }

    private void fail(final Session session, final Throwable error) {
        if (!session.failed.compareAndSet(false, true)) return;
        removeHandler(session);
        onServer(() -> {
            if (currentConnection(session)) notifyOwners(session.player, error);
        });
    }

    private void notifyOwners(Player player, Throwable error) {
        lifecycleOwner.getLogger().log(Level.WARNING, "Client observation stopped for " + player.getUniqueId(), cause(error));
        for (Registration registration : registrations) {
            if (registration.closed.get() || !registration.owner.isEnabled()) continue;
            try { registration.failure.accept(player); }
            catch (RuntimeException callbackError) {
                lifecycleOwner.getLogger().log(Level.WARNING, "Client stream failure callback failed for " + registration.owner.getName(), callbackError);
            }
        }
    }

    private void detach(Session session) {
        session.closed.set(true);
        removeHandler(session);
    }

    private void removeHandler(final Session session) {
        if (session.handler == null || !session.removing.compareAndSet(false, true)) return;
        try {
            binding.execute(session.connection.channel, () -> {
                try {
                    Object pipeline = binding.pipeline.invoke(session.connection.channel);
                    // Identity check prevents retiring an unrelated/new handler.
                    if (binding.getHandler.invoke(pipeline, session.handlerName) == session.handler)
                        binding.removeHandler.invoke(pipeline, session.handlerName);
                } catch (Throwable error) {
                    lifecycleOwner.getLogger().log(Level.FINE, "Could not remove retired client observer", cause(error));
                }
            });
        } catch (ReflectiveOperationException | RuntimeException error) {
            lifecycleOwner.getLogger().log(Level.FINE, "Client observer channel already retired", cause(error));
        }
    }

    private void onServer(Runnable task) {
        if (Bukkit.isPrimaryThread()) { task.run(); return; }
        try { if (!shutdown && lifecycleOwner.isEnabled()) SchedulerServices.get().runGlobal(lifecycleOwner, task); }
        catch (RuntimeException error) { lifecycleOwner.getLogger().log(Level.FINE, "Client observer cleanup stopped during shutdown", error); }
    }

    private static void requireServerThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Client event operations require the server thread");
    }

    /** The unused service must not invalidate BeastLib's existing Folia lifecycle. */
    private static void requireLifecycleThread() {
        if (SchedulerServices.isAvailable() && SchedulerServices.get().isFolia()) return;
        requireServerThread();
    }

    private static Throwable cause(Throwable error) {
        return error instanceof InvocationTargetException && error.getCause() != null ? error.getCause() : error;
    }

    private interface PaperCallback { void accept(Event event) throws ReflectiveOperationException; }

    private final class Registration implements ClientEventRegistration {
        final Plugin owner;
        final Consumer<Player> failure;
        final AtomicBoolean closed = new AtomicBoolean();
        Registration(Plugin owner, Consumer<Player> failure) { this.owner = owner; this.failure = failure; }
        @Override public boolean isClosed() { return closed.get(); }
        @Override public void close() {
            // CopyOnWriteArrayList removal is safe even when an owner closes
            // from a worker; the service immediately releases its reference.
            if (closed.compareAndSet(false, true)) registrations.remove(this);
        }
    }

    private static final class Session {
        final Player player;
        final NativeClientBinding.ConnectionIdentity connection;
        final String handlerName;
        final AtomicBoolean closed = new AtomicBoolean(), failed = new AtomicBoolean(), removing = new AtomicBoolean();
        ClientEventDispatcher dispatcher;
        final Set<Long> sentChunks = new LinkedHashSet<Long>();
        volatile UUID wireWorld;
        volatile long wireEpoch;
        UUID sentWorld;
        boolean snapshotFailureLogged;
        Object handler;
        Session(Player player, NativeClientBinding.ConnectionIdentity connection, long generation) {
            this.player = player; this.connection = connection;
            this.handlerName = "beastlib_client_" + generation;
            this.wireWorld = player.getWorld().getUID(); this.sentWorld = wireWorld;
        }
    }

    private static final class Observation {
        static final int TICK = 0, USE = 1, CHUNK = 2, FORGET = 3, RESET = 4;
        final int kind, x, z;
        final UUID world;
        final long epoch, watchEpoch;
        final boolean attack;
        final EquipmentSlot hand;
        final Vector clicked;
        private Observation(Session session, int kind, long watchEpoch, int x, int z, boolean attack, EquipmentSlot hand, Vector clicked) {
            this.world = session.wireWorld; this.epoch = session.wireEpoch;
            this.watchEpoch = watchEpoch;
            this.kind = kind; this.x = x; this.z = z; this.attack = attack; this.hand = hand;
            this.clicked = clicked == null ? null : clicked.clone();
        }
        static Observation tick(Session session, long watchEpoch) { return new Observation(session, TICK, watchEpoch, 0, 0, false, null, null); }
        static Observation reset(Session session) { return new Observation(session, RESET, 0, 0, 0, false, null, null); }
        static Observation use(Session session, long watchEpoch, int id, boolean attack, EquipmentSlot hand, Vector at) {
            return new Observation(session, USE, watchEpoch, id, 0, attack, hand, at);
        }
        static Observation chunk(Session session, int x, int z, boolean forget) {
            return new Observation(session, forget ? FORGET : CHUNK, 0, x, z, false, null, null);
        }
    }
}
