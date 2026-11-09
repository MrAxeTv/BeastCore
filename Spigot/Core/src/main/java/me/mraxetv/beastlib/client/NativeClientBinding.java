package me.mraxetv.beastlib.client;

import me.mraxetv.beastlib.api.reflection.MinecraftReflection;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/** Lazy, reflection-only bindings for the supported modern client protocols. */
final class NativeClientBinding {
    private final MinecraftReflection names;
    final int protocol;
    final Class<?> inbound, outbound, futureListener;
    final Class<?> tickPacket, attackPacket, interactPacket, chunkPacket, forgetPacket;
    final Class<?> loginPacket, respawnPacket, bundlePacket;
    final Method playerHandle, worldHandle, pipeline, eventLoop, addBefore, getHandler, removeHandler;
    final Method fireRead, write, unvoid, addFutureListener, futureSuccess, channelOpen;
    final Method chunkX, chunkZ, forgetPosition, loginInfo, respawnInfo, spawnDimension;
    final Method worldDimension, keyName, bundlePackets, entityLookup, attackId, interactId, interactHand, interactLocation;
    final Method getServer, serverExecute;
    final Field combinedEntityId;
    final Method combinedDispatch;
    final Class<?> combinedHandler;
    final Field listenerField, connectionField, channelField, positionX, positionZ, vectorX, vectorY, vectorZ;
    final Map<String, Method> forwards = new HashMap<String, Method>();

    NativeClientBinding(ClassLoader loader) throws ReflectiveOperationException {
        names = new MinecraftReflection(loader);
        Class<?> constants = type(loader, "net.minecraft.SharedConstants");
        protocol = ((Number) names.method(constants, "getProtocolVersion").invoke(null)).intValue();
        if (protocol < 773 || protocol > 777)
            throw new IllegalStateException("Client packet observations support Minecraft 1.21.9 through 26.3 (773..777), found " + protocol);
        Class<?> minecraftServer = type(loader, "net.minecraft.server.MinecraftServer");
        getServer = names.method(minecraftServer, "getServer");
        // Uses the native FIFO, but rejects a stopped server instead of the
        // inherited execute method's possible inline execution during stop.
        serverExecute = names.method(minecraftServer, "executeIfPossible", Runnable.class);

        Class<?> serverPlayer = type(loader, "net.minecraft.server.level.ServerPlayer");
        Class<?> commonListener = type(loader, "net.minecraft.server.network.ServerCommonPacketListenerImpl");
        Class<?> connection = type(loader, "net.minecraft.network.Connection");
        Class<?> channel = type(loader, "io.netty.channel.Channel");
        Class<?> pipelineType = type(loader, "io.netty.channel.ChannelPipeline");
        Class<?> handler = type(loader, "io.netty.channel.ChannelHandler");
        Class<?> context = type(loader, "io.netty.channel.ChannelHandlerContext");
        Class<?> promise = type(loader, "io.netty.channel.ChannelPromise");
        futureListener = type(loader, "io.netty.util.concurrent.GenericFutureListener");
        Class<?> future = type(loader, "io.netty.util.concurrent.Future");
        inbound = type(loader, "io.netty.channel.ChannelInboundHandler");
        outbound = type(loader, "io.netty.channel.ChannelOutboundHandler");
        playerHandle = names.craftClass("entity.CraftPlayer").getMethod("getHandle");
        worldHandle = names.craftClass("CraftWorld").getMethod("getHandle");
        listenerField = field(serverPlayer, "connection");
        connectionField = field(commonListener, "connection");
        channelField = field(connection, "channel");
        pipeline = channel.getMethod("pipeline");
        eventLoop = channel.getMethod("eventLoop");
        channelOpen = channel.getMethod("isOpen");
        addBefore = pipelineType.getMethod("addBefore", String.class, String.class, handler);
        getHandler = pipelineType.getMethod("get", String.class);
        removeHandler = pipelineType.getMethod("remove", String.class);
        fireRead = context.getMethod("fireChannelRead", Object.class);
        write = context.getMethod("write", Object.class, promise);
        unvoid = promise.getMethod("unvoid");
        addFutureListener = promise.getMethod("addListener", futureListener);
        futureSuccess = future.getMethod("isSuccess");

        tickPacket = game(loader, "ServerboundClientTickEndPacket");
        Class<?> splitAttack;
        try { splitAttack = game(loader, "ServerboundAttackPacket"); }
        catch (ClassNotFoundException combinedInteractionProtocol) { splitAttack = null; }
        attackPacket = splitAttack;
        interactPacket = game(loader, "ServerboundInteractPacket");
        if (attackPacket != null) {
            attackId = names.method(attackPacket, "entityId");
            interactId = names.method(interactPacket, "entityId");
            interactHand = names.method(interactPacket, "hand");
            interactLocation = names.method(interactPacket, "location");
            combinedEntityId = null;
            combinedDispatch = null;
            combinedHandler = null;
        } else {
            attackId = null;
            interactId = null;
            interactHand = null;
            interactLocation = null;
            combinedEntityId = field(interactPacket, "entityId");
            combinedHandler = game(loader, "ServerboundInteractPacket$Handler");
            combinedDispatch = names.method(interactPacket, "dispatch", combinedHandler);
        }
        Class<?> vector = type(loader, "net.minecraft.world.phys.Vec3");
        vectorX = field(vector, "x"); vectorY = field(vector, "y"); vectorZ = field(vector, "z");

        chunkPacket = game(loader, "ClientboundLevelChunkWithLightPacket");
        chunkX = names.method(chunkPacket, protocol == 777 ? "x" : "getX");
        chunkZ = names.method(chunkPacket, protocol == 777 ? "z" : "getZ");
        forgetPacket = game(loader, "ClientboundForgetLevelChunkPacket");
        forgetPosition = names.method(forgetPacket, "pos");
        Class<?> chunkPosition = type(loader, "net.minecraft.world.level.ChunkPos");
        positionX = field(chunkPosition, "x"); positionZ = field(chunkPosition, "z");
        loginPacket = game(loader, "ClientboundLoginPacket");
        respawnPacket = game(loader, "ClientboundRespawnPacket");
        loginInfo = names.method(loginPacket, "commonPlayerSpawnInfo");
        respawnInfo = names.method(respawnPacket, "commonPlayerSpawnInfo");
        spawnDimension = names.method(game(loader, "CommonPlayerSpawnInfo"), "dimension");
        Class<?> level = type(loader, "net.minecraft.world.level.Level");
        worldDimension = names.method(level, "dimension");
        entityLookup = names.method(level, "getEntity", int.class);
        Class<?> key = type(loader, "net.minecraft.resources.ResourceKey");
        Method keyAccessor;
        try { keyAccessor = names.method(key, "identifier"); }
        catch (NoSuchMethodException olderIdentifierName) { keyAccessor = names.method(key, "location"); }
        keyName = keyAccessor;
        bundlePacket = game(loader, "ClientboundBundlePacket");
        bundlePackets = names.method(bundlePacket, "subPackets");

        for (String name : Arrays.asList("channelRegistered", "channelUnregistered", "channelActive", "channelInactive",
                "channelReadComplete", "channelWritabilityChanged"))
            forwards.put(name, context.getMethod("fire" + Character.toUpperCase(name.charAt(0)) + name.substring(1)));
        forwards.put("exceptionCaught", context.getMethod("fireExceptionCaught", Throwable.class));
        forwards.put("userEventTriggered", context.getMethod("fireUserEventTriggered", Object.class));
        forwards.put("bind", context.getMethod("bind", SocketAddress.class, promise));
        forwards.put("connect", context.getMethod("connect", SocketAddress.class, SocketAddress.class, promise));
        for (String name : Arrays.asList("disconnect", "close", "deregister"))
            forwards.put(name, context.getMethod(name, promise));
        for (String name : Arrays.asList("read", "flush")) forwards.put(name, context.getMethod(name));
    }

    ConnectionIdentity connection(Player player) throws ReflectiveOperationException {
        Object handle = playerHandle.invoke(player);
        Object listener = listenerField.get(handle);
        Object connection = connectionField.get(listener);
        return new ConnectionIdentity(handle, listener, connection, channelField.get(connection));
    }

    boolean matches(Player player, ConnectionIdentity expected) throws ReflectiveOperationException {
        Object handle = playerHandle.invoke(player);
        return handle == expected.handle && listenerField.get(handle) == expected.listener
                && connectionField.get(expected.listener) == expected.connection
                && channelField.get(expected.connection) == expected.channel
                && Boolean.TRUE.equals(channelOpen.invoke(expected.channel));
    }

    String worldKey(World world) throws ReflectiveOperationException {
        return dimensionKey(worldDimension.invoke(worldHandle.invoke(world)));
    }

    String dimensionKey(Object resourceKey) throws ReflectiveOperationException {
        return keyName.invoke(resourceKey).toString();
    }

    boolean entityExists(World world, int entityId) throws ReflectiveOperationException {
        return entityLookup.invoke(worldHandle.invoke(world), entityId) != null;
    }

    Vector clickedPosition(Object packet) throws ReflectiveOperationException {
        Object value = interactLocation.invoke(packet);
        return clickedVector(value);
    }

    private Vector clickedVector(Object value) throws ReflectiveOperationException {
        if (value == null) return null;
        return new Vector(vectorX.getDouble(value), vectorY.getDouble(value), vectorZ.getDouble(value));
    }

    EquipmentSlot hand(Object packet) throws ReflectiveOperationException {
        return interactionHand(interactHand.invoke(packet));
    }

    private EquipmentSlot interactionHand(Object value) {
        if (!(value instanceof Enum<?>)) throw new IllegalStateException("Unknown native interaction hand");
        String name = ((Enum<?>) value).name();
        if ("MAIN_HAND".equals(name)) return EquipmentSlot.HAND;
        if ("OFF_HAND".equals(name)) return EquipmentSlot.OFF_HAND;
        // Obfuscated Spigot enum field/name strings retain the protocol's two ordered hands.
        Enum<?> hand = (Enum<?>) value;
        if (hand.getDeclaringClass().getEnumConstants().length == 2)
            return hand.ordinal() == 0 ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
        throw new IllegalStateException("Unsupported native interaction hand " + name);
    }

    /** Decode either the older combined use packet or the newer split attack packet. */
    UnknownUse unknownUse(Object packet) throws ReflectiveOperationException {
        if (attackPacket != null && attackPacket.isInstance(packet))
            return new UnknownUse(((Number) attackId.invoke(packet)).intValue(), true, null, null);
        if (!interactPacket.isInstance(packet)) return null;
        if (combinedDispatch == null)
            return new UnknownUse(((Number) interactId.invoke(packet)).intValue(), false, hand(packet), clickedPosition(packet));
        final int entity = combinedEntityId.getInt(packet);
        final UnknownUse[] result = new UnknownUse[1];
        Object handler = Proxy.newProxyInstance(combinedHandler.getClassLoader(), new Class<?>[]{combinedHandler},
                (proxy, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        if ("toString".equals(method.getName())) return "BeastLib interaction decoder";
                        if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                        if ("equals".equals(method.getName())) return proxy == arguments[0];
                    }
                    int count = arguments == null ? 0 : arguments.length;
                    if (count == 0) result[0] = new UnknownUse(entity, true, null, null);
                    else if (count == 1) result[0] = new UnknownUse(entity, false, interactionHand(arguments[0]), null);
                    else if (count == 2) result[0] = new UnknownUse(entity, false, interactionHand(arguments[0]), clickedVector(arguments[1]));
                    else throw new IllegalStateException("Unsupported native entity interaction action");
                    return null;
                });
        combinedDispatch.invoke(packet, handler);
        if (result[0] == null) throw new IllegalStateException("Native entity interaction did not identify its action");
        return result[0];
    }

    void execute(Object channel, Runnable task) throws ReflectiveOperationException {
        ((Executor) eventLoop.invoke(channel)).execute(task);
    }

    /** Same FIFO used by native packet handling, after downstream channelRead has queued its work. */
    void executeOnServer(Runnable task) throws ReflectiveOperationException {
        Object server = getServer.invoke(null);
        if (server == null) throw new IllegalStateException("Native server executor is unavailable");
        serverExecute.invoke(server, task);
    }

    private Class<?> type(ClassLoader loader, String name) throws ClassNotFoundException {
        return names.type(name);
    }

    private Class<?> game(ClassLoader loader, String name) throws ClassNotFoundException {
        return type(loader, "net.minecraft.network.protocol.game." + name);
    }

    private Field field(Class<?> type, String name) throws NoSuchFieldException {
        return names.field(type, name);
    }

    static final class UnknownUse {
        final int entity;
        final boolean attack;
        final EquipmentSlot hand;
        final Vector clicked;
        UnknownUse(int entity, boolean attack, EquipmentSlot hand, Vector clicked) {
            this.entity = entity; this.attack = attack; this.hand = hand; this.clicked = clicked;
        }
    }

    static final class ConnectionIdentity {
        final Object handle, listener, connection, channel;
        ConnectionIdentity(Object handle, Object listener, Object connection, Object channel) {
            this.handle = handle; this.listener = listener; this.connection = connection; this.channel = channel;
        }
    }
}
