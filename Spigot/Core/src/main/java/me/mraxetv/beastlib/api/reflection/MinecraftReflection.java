package me.mraxetv.beastlib.api.reflection;

import org.bukkit.Server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves Mojang member names on supported CraftBukkit runtimes.
 * Lookups remain lazy: constructing an unused BeastLib service never resolves NMS.
 */
public final class MinecraftReflection {
    private static final ConcurrentHashMap<String, Properties> TABLES = new ConcurrentHashMap<String, Properties>();
    private final ClassLoader loader;
    private final String craftPackage;
    private final Properties aliases;

    public MinecraftReflection(ClassLoader loader) {
        this(loader, findCraftPackage(Objects.requireNonNull(loader, "loader")));
    }

    public MinecraftReflection(ClassLoader loader, String craftPackage) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.craftPackage = Objects.requireNonNull(craftPackage, "craftPackage");
        if (!craftPackage.equals("org.bukkit.craftbukkit")
                && !craftPackage.startsWith("org.bukkit.craftbukkit."))
            throw new IllegalArgumentException("Expected a CraftBukkit package");
        String revision = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
        String resource = "v1_21_R6".equals(revision) ? "spigot-1.21.10.properties"
                : "v1_21_R7".equals(revision) ? "spigot-1.21.11.properties" : null;
        aliases = resource == null ? new Properties() : aliases(resource);
    }

    private static Properties aliases(String resource) {
        Properties cached = TABLES.get(resource);
        if (cached != null) return cached;
        Properties loaded = new Properties();
        try (InputStream input = MinecraftReflection.class.getResourceAsStream("/me/mraxetv/beastlib/reflection/" + resource)) {
            if (input == null) throw new IllegalStateException("Missing native reflection aliases: " + resource);
            loaded.load(input);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read native reflection aliases: " + resource, failure);
        }
        Properties prior = TABLES.putIfAbsent(resource, loaded);
        return prior == null ? loaded : prior;
    }

    public static MinecraftReflection forServer(Server server) {
        Objects.requireNonNull(server, "server");
        Class<?> implementation = server.getClass();
        return new MinecraftReflection(implementation.getClassLoader(), implementation.getPackage().getName());
    }

    public String craftPackage() { return craftPackage; }

    public Class<?> craftClass(String relativeName) throws ClassNotFoundException {
        return Class.forName(craftPackage + "." + Objects.requireNonNull(relativeName, "relativeName"), false, loader);
    }

    public Class<?> type(String mojangName) throws ClassNotFoundException {
        Objects.requireNonNull(mojangName, "mojangName");
        String translated = aliases.getProperty("C." + mojangName);
        if (translated != null && !translated.equals(mojangName)) {
            try { return Class.forName(translated, false, loader); }
            catch (ClassNotFoundException absent) { /* A mapped fork can retain Mojang class names. */ }
        }
        return Class.forName(mojangName, false, loader);
    }

    /** Public methods, inherited declarations and explicitly requested private members. */
    public Method method(Class<?> owner, String mojangName, Class<?>... parameters) throws NoSuchMethodException {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(mojangName, "mojangName");
        Objects.requireNonNull(parameters, "parameters");
        ArrayDeque<Class<?>> pending = new ArrayDeque<Class<?>>();
        Set<Class<?>> visited = new LinkedHashSet<Class<?>>();
        pending.add(owner);
        while (!pending.isEmpty()) {
            Class<?> current = pending.removeFirst();
            if (!visited.add(current)) continue;
            String translated = aliases.getProperty(methodKey(current, mojangName, parameters));
            if (translated != null) {
                for (String candidate : translated.split("\\|")) {
                    try { return accessible(current.getDeclaredMethod(candidate, parameters)); }
                    catch (NoSuchMethodException absent) { /* Continue through the actual inheritance graph. */ }
                }
            }
            if (current.getSuperclass() != null) pending.addLast(current.getSuperclass());
            for (Class<?> parent : current.getInterfaces()) pending.addLast(parent);
        }
        try { return accessible(owner.getMethod(mojangName, parameters)); }
        catch (NoSuchMethodException absent) { /* Stable added methods can be non-public. */ }
        for (Class<?> current : visited) {
            try { return accessible(current.getDeclaredMethod(mojangName, parameters)); }
            catch (NoSuchMethodException absent) { /* Continue through parent declarations. */ }
        }
        throw new NoSuchMethodException(methodKey(owner, mojangName, parameters));
    }

    /** Field aliases use the actual declaring class, including inherited private data IDs. */
    public Field field(Class<?> owner, String mojangName) throws NoSuchFieldException {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(mojangName, "mojangName");
        ArrayDeque<Class<?>> pending = new ArrayDeque<Class<?>>();
        Set<Class<?>> visited = new LinkedHashSet<Class<?>>();
        pending.add(owner);
        while (!pending.isEmpty()) {
            Class<?> current = pending.removeFirst();
            if (!visited.add(current)) continue;
            String translated = aliases.getProperty("F." + current.getName() + "#" + mojangName);
            if (translated != null) {
                try { return accessible(current.getDeclaredField(translated)); }
                catch (NoSuchFieldException absent) { /* Continue through parent declarations. */ }
            }
            if (current.getSuperclass() != null) pending.addLast(current.getSuperclass());
            for (Class<?> parent : current.getInterfaces()) pending.addLast(parent);
        }
        for (Class<?> current : visited) {
            try { return accessible(current.getDeclaredField(mojangName)); }
            catch (NoSuchFieldException absent) { /* Continue through parent declarations. */ }
        }
        throw new NoSuchFieldException(owner.getName() + "." + mojangName);
    }

    private static String methodKey(Class<?> owner, String name, Class<?>[] parameters) {
        StringBuilder key = new StringBuilder("M.").append(owner.getName()).append('#').append(name).append('(');
        for (int index = 0; index < parameters.length; index++) {
            if (index != 0) key.append(',');
            key.append(parameters[index].getName());
        }
        return key.append(')').toString();
    }

    private static Method accessible(Method method) {
        if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))
            method.setAccessible(true);
        return method;
    }

    private static Field accessible(Field field) {
        if (!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers()))
            field.setAccessible(true);
        return field;
    }

    private static String findCraftPackage(ClassLoader loader) {
        for (String candidate : new String[]{"org.bukkit.craftbukkit", "org.bukkit.craftbukkit.v1_21_R6",
                "org.bukkit.craftbukkit.v1_21_R7"}) {
            try {
                Class.forName(candidate + ".CraftServer", false, loader);
                return candidate;
            } catch (ClassNotFoundException absent) { /* Try the next supported package shape. */ }
        }
        throw new IllegalStateException("Cannot locate a supported CraftBukkit server package");
    }
}
