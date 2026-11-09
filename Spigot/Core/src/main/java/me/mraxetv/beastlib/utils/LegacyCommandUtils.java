package me.mraxetv.beastlib.utils;

import me.mraxetv.beastlib.api.BeastLibAPI;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

public final class LegacyCommandUtils {

    private LegacyCommandUtils() {
    }

    public static void applyLegacyCommandFix(JavaPlugin plugin, Command command) {
        if (plugin == null || command == null) {
            return;
        }
        if (!MCVersion.getVersion().equals(MCVersion.MC1_8_R3)) {
            return;
        }

        try {
            Field timingsField = findField(command.getClass(), "timings");
            if (timingsField == null) {
                return;
            }

            timingsField.setAccessible(true);
            if (timingsField.get(command) != null) {
                return;
            }

            Class<?> timingClass = Class.forName("co.aikar.timings.Timing");
            Object noOpTiming = Proxy.newProxyInstance(
                    timingClass.getClassLoader(),
                    new Class[]{timingClass},
                    (proxy, method, args) -> getDefaultValue(method.getReturnType(), proxy)
            );
            timingsField.set(command, noOpTiming);
        } catch (Throwable throwable) {
            if (plugin instanceof BeastLibAPI) {
                throwable.printStackTrace();
            }
        }
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Object getDefaultValue(Class<?> type, Object proxy) {
        if (type == Void.TYPE) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return false;
        }
        if (type == Byte.TYPE) {
            return (byte) 0;
        }
        if (type == Short.TYPE) {
            return (short) 0;
        }
        if (type == Integer.TYPE) {
            return 0;
        }
        if (type == Long.TYPE) {
            return 0L;
        }
        if (type == Float.TYPE) {
            return 0F;
        }
        if (type == Double.TYPE) {
            return 0D;
        }
        if (type == Character.TYPE) {
            return '\0';
        }
        if (type.isInstance(proxy)) {
            return proxy;
        }
        return null;
    }
}
