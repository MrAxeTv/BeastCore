package me.mraxetv.beastlib.client;

import me.mraxetv.beastlib.api.client.ClientDisplayService;
import me.mraxetv.beastlib.api.event.player.PlayerDialogResponseEvent;
import me.mraxetv.beastlib.api.reflection.MinecraftReflection;
import me.mraxetv.beastlib.api.scheduler.SchedulerServices;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Native JSON display transport. Modern bindings are resolved only when used. */
public final class BukkitClientDisplayService implements ClientDisplayService {
    private final Plugin owner;
    private final Listener dialogListener = new Listener() { };
    private volatile boolean shutdown;
    private boolean started;
    private volatile long generation;
    private volatile Bindings bindings;

    public BukkitClientDisplayService(Plugin owner) {
        if (owner == null) throw new IllegalArgumentException("owner cannot be null");
        this.owner = owner;
    }

    /** Installs the optional native dialog callback adapter; safe on legacy APIs. */
    @SuppressWarnings("unchecked")
    public synchronized void start() {
        checkOpen();
        if (started) return;
        started = true;
        final long expectedGeneration = ++generation;
        try {
            Class<?> candidate = Class.forName("org.bukkit.event.player.PlayerCustomClickEvent", false,
                    owner.getServer().getClass().getClassLoader());
            if (!Event.class.isAssignableFrom(candidate)) return;
            final Method player = candidate.getMethod("getPlayer");
            final Method action = candidate.getMethod("getId");
            final Method payload = candidate.getMethod("getData");
            owner.getServer().getPluginManager().registerEvent((Class<? extends Event>) candidate, dialogListener,
                    EventPriority.MONITOR, new EventExecutor() {
                        @Override public void execute(Listener listener, final Event event) throws EventException {
                            if (shutdown || generation != expectedGeneration) return;
                            try {
                                final Player receiver = (Player) player.invoke(event);
                                final String actionId = String.valueOf(action.invoke(event));
                                Object data = payload.invoke(event);
                                final String json = data == null ? "null" : data.toString();
                                Runnable forward = new Runnable() {
                                    @Override public void run() {
                                        if (shutdown || generation != expectedGeneration || !receiver.isOnline()
                                                || owner.getServer().getPlayer(receiver.getUniqueId()) != receiver) return;
                                        owner.getServer().getPluginManager().callEvent(
                                                new PlayerDialogResponseEvent(receiver, actionId, json));
                                    }
                                };
                                if (event.isAsynchronous()) SchedulerServices.get().runPlayer(owner, receiver, forward);
                                else forward.run();
                            } catch (ReflectiveOperationException failure) { throw new EventException(failure); }
                        }
                    }, owner, true);
        } catch (ClassNotFoundException absentOnLegacyServer) {
            // Dialog support is optional; no modern class is linked during enable.
        } catch (ReflectiveOperationException failure) {
            owner.getLogger().warning("Client dialog callbacks are unavailable: " + failure.getMessage());
        }
    }

    public synchronized void shutdown() {
        if (shutdown) return;
        shutdown = true;
        generation++;
        HandlerList.unregisterAll(dialogListener);
        bindings = null;
    }

    @Override public void sendMessage(CommandSender receiver, String json) {
        require(receiver, "receiver");
        if (receiver instanceof Player) {
            sendComponent((Player) receiver, json, "ClientboundSystemChatPacket", true);
            return;
        }
        checkOpen();
        try { bindings().console().send(receiver, requireJson(json)); }
        catch (ReflectiveOperationException failure) { throw unavailable("JSON message", failure); }
    }

    @Override public void sendActionBar(Player player, String json) {
        sendComponent(player, json, "ClientboundSetActionBarTextPacket", false);
    }
    @Override public void sendTitle(Player player, String json) {
        sendComponent(player, json, "ClientboundSetTitleTextPacket", false);
    }
    @Override public void sendSubtitle(Player player, String json) {
        sendComponent(player, json, "ClientboundSetSubtitleTextPacket", false);
    }

    private void sendComponent(Player player, String json, String packetName, boolean systemMessage) {
        require(player, "player"); checkOpen();
        try {
            Bindings current = bindings();
            Components components = current.components();
            Object component = components.read(requireJson(json));
            Constructor<?> constructor = current.packet(packetName, systemMessage
                    ? new Class<?>[]{components.type, boolean.class} : new Class<?>[]{components.type});
            Object packet = systemMessage ? constructor.newInstance(component, false) : constructor.newInstance(component);
            current.transport().send(player, packet);
        } catch (ReflectiveOperationException failure) { throw unavailable("JSON display component", failure); }
    }

    @Override public void setTitleTimes(Player player, int fadeIn, int stay, int fadeOut) {
        require(player, "player"); checkOpen();
        try {
            Bindings current = bindings();
            Object packet = current.packet("ClientboundSetTitlesAnimationPacket", int.class, int.class, int.class)
                    .newInstance(fadeIn, stay, fadeOut);
            current.transport().send(player, packet);
        } catch (ReflectiveOperationException failure) { throw unavailable("title timing", failure); }
    }

    @Override public void clearTitle(Player player, boolean reset) {
        require(player, "player"); checkOpen();
        try {
            Bindings current = bindings();
            Object packet = current.packet("ClientboundClearTitlesPacket", boolean.class).newInstance(reset);
            current.transport().send(player, packet);
        } catch (ReflectiveOperationException failure) { throw unavailable("title clearing", failure); }
    }

    @Override public void setDisplayText(Entity entity, String json) {
        require(entity, "entity"); checkOpen();
        try {
            Bindings current = bindings();
            current.display().set(entity, current.components().read(requireJson(json)));
        } catch (ReflectiveOperationException failure) { throw unavailable("text display", failure); }
    }

    @Override public void showDialog(Player player, String json) {
        require(player, "player"); checkOpen();
        try { bindings().dialogs().show(player, requireJson(json)); }
        catch (ReflectiveOperationException failure) { throw unavailable("dialog", failure); }
    }

    @Override public void closeDialog(Player player) {
        require(player, "player"); checkOpen();
        try { player.getClass().getMethod("clearDialog").invoke(player); }
        catch (ReflectiveOperationException failure) { throw unavailable("dialog clearing", failure); }
    }

    private Bindings bindings() {
        Bindings current = bindings;
        if (current != null) return current;
        synchronized (this) {
            if (bindings == null) {
                Class<?> server = owner.getServer().getClass();
                String craftPackage = server.getPackage().getName();
                if (!craftPackage.startsWith("org.bukkit.craftbukkit"))
                    throw new IllegalStateException("Native client display transport requires CraftBukkit.");
                bindings = new Bindings(server.getClassLoader(), craftPackage);
            }
            return bindings;
        }
    }

    private void checkOpen() {
        if (shutdown) throw new IllegalStateException("BeastLib client display service is shut down.");
    }

    private static <T> T require(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " cannot be null");
        return value;
    }
    private static String requireJson(String json) {
        require(json, "json");
        if (json.trim().isEmpty()) throw new IllegalArgumentException("json cannot be empty");
        return json;
    }
    private static IllegalStateException unavailable(String feature, ReflectiveOperationException failure) {
        Throwable cause = failure instanceof InvocationTargetException && failure.getCause() != null
                ? failure.getCause() : failure;
        return new IllegalStateException("Native " + feature + " is unavailable or rejected its JSON payload.", cause);
    }

    /** Each feature owns its lookup, so missing newer APIs never block legacy startup. */
    private static final class Bindings {
        final ClassLoader loader;
        final String craft;
        final MinecraftReflection names;
        final Map<String, Constructor<?>> packets = new HashMap<String, Constructor<?>>();
        Components components;
        Transport transport;
        Display display;
        Dialogs dialogs;
        Console console;

        Bindings(ClassLoader loader, String craft) {
            this.loader = loader;
            this.craft = craft;
            this.names = new MinecraftReflection(loader, craft);
        }
        Class<?> type(String name) throws ClassNotFoundException { return names.type(name); }
        synchronized Components components() throws ReflectiveOperationException {
            if (components == null) components = new Components(type(craft + ".util.CraftChatMessage"));
            return components;
        }
        synchronized Transport transport() throws ReflectiveOperationException {
            if (transport == null) transport = new Transport(names, type(craft + ".entity.CraftPlayer"), type("net.minecraft.network.protocol.Packet"));
            return transport;
        }
        synchronized Constructor<?> packet(String name, Class<?>... arguments) throws ReflectiveOperationException {
            Constructor<?> constructor = packets.get(name);
            if (constructor == null) {
                constructor = type("net.minecraft.network.protocol.game." + name).getConstructor(arguments);
                packets.put(name, constructor);
            }
            return constructor;
        }
        synchronized Display display() throws ReflectiveOperationException {
            if (display == null) display = new Display(names, type(craft + ".entity.CraftEntity"),
                    type("net.minecraft.world.entity.Display$TextDisplay"), components().type);
            return display;
        }
        synchronized Dialogs dialogs() throws ReflectiveOperationException {
            if (dialogs == null) dialogs = new Dialogs(loader, names, type(craft + ".entity.CraftPlayer"));
            return dialogs;
        }
        synchronized Console console() throws ReflectiveOperationException {
            if (console == null) console = new Console(type("net.md_5.bungee.chat.ComponentSerializer"),
                    type("net.md_5.bungee.api.chat.BaseComponent"));
            return console;
        }
    }

    private static final class Components {
        final Method fromJson;
        final Class<?> type;
        Components(Class<?> craftMessages) throws NoSuchMethodException {
            fromJson = craftMessages.getMethod("fromJSON", String.class);
            type = fromJson.getReturnType();
        }
        Object read(String json) throws ReflectiveOperationException {
            Object component = fromJson.invoke(null, json);
            if (component == null) throw new IllegalArgumentException("JSON did not contain a text component.");
            return component;
        }
    }

    private static final class Transport {
        final Method handle, send;
        final Field connection;
        Transport(MinecraftReflection names, Class<?> player, Class<?> packet) throws ReflectiveOperationException {
            handle = player.getMethod("getHandle");
            connection = names.field(handle.getReturnType(), "connection");
            send = names.method(connection.getType(), "send", packet);
        }
        void send(Player player, Object packet) throws ReflectiveOperationException {
            Object listener = connection.get(handle.invoke(player));
            if (listener == null) throw new IllegalStateException("Player has no active native connection.");
            send.invoke(listener, packet);
        }
    }

    private static final class Display {
        final Method handle, setText;
        final Class<?> textDisplay;
        Display(MinecraftReflection names, Class<?> entity, Class<?> textDisplay, Class<?> component) throws NoSuchMethodException {
            this.textDisplay = textDisplay;
            handle = entity.getMethod("getHandle");
            setText = names.method(textDisplay, "setText", component);
        }
        void set(Entity entity, Object component) throws ReflectiveOperationException {
            Object nativeEntity = handle.invoke(entity);
            if (!textDisplay.isInstance(nativeEntity)) throw new IllegalArgumentException("Entity is not a text display.");
            setText.invoke(nativeEntity, component);
        }
    }

    private static final class Dialogs {
        final Method handle, level, registry, createContext, parseJson, parse, result, holder, open;
        final Field jsonOps, codec;
        Dialogs(ClassLoader loader, MinecraftReflection names, Class<?> player) throws ReflectiveOperationException {
            handle = player.getMethod("getHandle");
            Class<?> nativePlayer = handle.getReturnType();
            level = names.method(nativePlayer, "level");
            registry = names.method(level.getReturnType(), "registryAccess");
            Class<?> dynamicOps = Class.forName("com.mojang.serialization.DynamicOps", false, loader);
            createContext = names.method(registry.getReturnType(), "createSerializationContext", dynamicOps);
            Class<?> ops = Class.forName("com.mojang.serialization.JsonOps", false, loader);
            jsonOps = ops.getField("INSTANCE");
            // Derive the server's Gson package; BeastLib's shaded Gson is a different type.
            Class<?> jsonElement = ops.getMethod("createString", String.class).getReturnType();
            Class<?> parser = Class.forName(jsonElement.getPackage().getName() + ".JsonParser", false, loader);
            parseJson = parser.getMethod("parseString", String.class);
            Class<?> dialog = names.type("net.minecraft.server.dialog.Dialog");
            codec = names.field(dialog, "DIRECT_CODEC");
            parse = codec.getType().getMethod("parse", dynamicOps, Object.class);
            result = parse.getReturnType().getMethod("getOrThrow");
            Class<?> nativeHolder = names.type("net.minecraft.core.Holder");
            holder = names.method(nativeHolder, "direct", Object.class);
            open = names.method(nativePlayer, "openDialog", nativeHolder);
        }
        void show(Player player, String json) throws ReflectiveOperationException {
            Object nativePlayer = handle.invoke(player);
            Object context = createContext.invoke(registry.invoke(level.invoke(nativePlayer)), jsonOps.get(null));
            Object decoded = parse.invoke(codec.get(null), context, parseJson.invoke(null, json));
            Object dialog = result.invoke(decoded);
            open.invoke(nativePlayer, holder.invoke(null, dialog));
        }
    }

    private static final class Console {
        final Method parse, spigot, send;
        Console(Class<?> serializer, Class<?> component) throws NoSuchMethodException {
            parse = serializer.getMethod("parse", String.class);
            spigot = CommandSender.class.getMethod("spigot");
            send = spigot.getReturnType().getMethod("sendMessage", Array.newInstance(component, 0).getClass());
        }
        void send(CommandSender receiver, String json) throws ReflectiveOperationException {
            send.invoke(spigot.invoke(receiver), parse.invoke(null, json));
        }
    }
}
