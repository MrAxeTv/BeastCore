package me.mraxetv.beastcore.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastcore.BeastCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class BUtils {


    private static BeastCore pl;

    public BUtils(BeastCore pl) {
        this.pl = pl;
    }

    public static boolean fullInv(Player p) {
        int check = p.getInventory().firstEmpty();
        return check == -1;
    }


    public static boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException efr) {
            return false;
        }
        return true;
    }

    public static boolean isInt(String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException efr) {
            return false;
        }
        return true;
    }

    public static boolean isString(String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException efr) {
            return true;
        }
        return false;
    }
    public static UUID toUUID(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {
        }

        return null;
    }
    public static boolean isItem(Material mat) {
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_12_R1)) return mat.isItem();
        else {
            Class myClass = null;
            try {
                myClass = Class.forName("net.minecraft.server.v1_8_R3.Item");
                Method method = myClass.getDeclaredMethod("getById", int.class);
                return method.invoke(null, mat.getId()) != null;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }
    public static void connect(Player p, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (Exception e) {
            e.printStackTrace();
        }

        p.sendPluginMessage(pl, "BungeeCord", out.toByteArray());
    }



}
