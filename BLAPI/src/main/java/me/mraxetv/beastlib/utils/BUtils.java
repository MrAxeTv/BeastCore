package me.mraxetv.beastlib.utils;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import me.mraxetv.beastlib.api.BeastLibAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BUtils<T extends JavaPlugin & BeastLibAPI> {


    private T pl;
    private String prefix;
    private static DebugLevel debugLevel = DebugLevel.LOWEST;
    private static HashSet<Material> leather;
    private static final char LEGACY_SECTION_CHAR = '\u00A7';
    private final static Pattern PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer HEX_LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character(LEGACY_SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();


    public BUtils(T pl) {
       this.pl = pl;
       prefix = pl.getMessages().getString("Prefix");
       //pl.getLogger().info(prefix);
    }

    static {
        if (MCVersion.isAtLeastVersion(MCVersion.MC1_8_R3)) {
            leather = new HashSet<>(Arrays.asList(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS));
            if (MCVersion.isAtLeastVersion(MCVersion.MC1_14_R1)) leather.add(Material.matchMaterial("LEATHER_HORSE_ARMOR"));
        } else leather = new HashSet<>();
    }

    public String getPrefix(){
        return prefix;
    }

    public void sendMessage(CommandSender sender, String message) {
        if (sender == null) {
            return;
        }

        if (message == null) {
            if (pl != null) {
                pl.getLogger().warning("Tried to send a null message to " + sender.getName());
            }
            return;
        }

        MessageType type = MessageType.CHAT;

        if (message.startsWith("[ACTION_BAR]")) {
            type = MessageType.ACTION_BAR;
            message = message.substring("[ACTION_BAR]".length()).trim();
        } else if (message.startsWith("[CHAT]")) {
            type = MessageType.CHAT;
            message = message.substring("[CHAT]".length()).trim();
        }

        switch (type) {
            case ACTION_BAR:
                if (!(sender instanceof Player)) {
                    sender.sendMessage(setPlaceholders(sender, message));
                    return;
                }

                ((Player) sender).spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(setPlaceholders(sender, message))
                );
                return;
            case CHAT:
            default:
                sender.sendMessage(setPlaceholders(sender, message));
        }
    }


    public abstract String setPlaceholders(CommandSender sender, String message);

    public static String setHexColors(String s) {
        if (!MCVersion.isAtLeastVersion(MCVersion.MC1_16_R1)) return s;
        Matcher matcher = PATTERN.matcher(s);
        while (matcher.find()) {
            String c = s.substring(matcher.start(), matcher.end());

            s = s.replace(c, "" + ChatColor.of(c.replace("&", "")));
            matcher = PATTERN.matcher(s);
        }
        return s;
    }

    public static String legacyToMiniMessage(String input) {
        return input
                .replaceAll("(?i)[&§]0", "<black>")
                .replaceAll("(?i)[&§]1", "<dark_blue>")
                .replaceAll("(?i)[&§]2", "<dark_green>")
                .replaceAll("(?i)[&§]3", "<dark_aqua>")
                .replaceAll("(?i)[&§]4", "<dark_red>")
                .replaceAll("(?i)[&§]5", "<dark_purple>")
                .replaceAll("(?i)[&§]6", "<gold>")
                .replaceAll("(?i)[&§]7", "<gray>")
                .replaceAll("(?i)[&§]8", "<dark_gray>")
                .replaceAll("(?i)[&§]9", "<blue>")
                .replaceAll("(?i)[&§]a", "<green>")
                .replaceAll("(?i)[&§]b", "<aqua>")
                .replaceAll("(?i)[&§]c", "<red>")
                .replaceAll("(?i)[&§]d", "<light_purple>")
                .replaceAll("(?i)[&§]e", "<yellow>")
                .replaceAll("(?i)[&§]f", "<white>")
                .replaceAll("(?i)[&§]k", "<obfuscated>")
                .replaceAll("(?i)[&§]l", "<bold>")
                .replaceAll("(?i)[&§]m", "<strikethrough>")
                .replaceAll("(?i)[&§]n", "<underlined>")
                .replaceAll("(?i)[&§]o", "<italic>")
                .replaceAll("(?i)[&§]r", "<reset>");
    }

    public static String normalizeLegacyHexColors(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder normalized = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (isLegacyColorPrefix(current)
                    && i + 1 < input.length()
                    && Character.toLowerCase(input.charAt(i + 1)) == 'x') {
                StringBuilder hex = new StringBuilder(6);
                int hexEnd = readLegacyHexColor(input, i, hex);

                if (hexEnd > -1) {
                    normalized.append("<#").append(hex).append(">");
                    i = hexEnd - 1;
                } else {
                    i++;
                }
                continue;
            }

            normalized.append(current);
        }

        return normalized.toString();
    }

    private static String removeInvalidLegacyHexMarkers(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder cleaned = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (isLegacyColorPrefix(current)
                    && i + 1 < input.length()
                    && Character.toLowerCase(input.charAt(i + 1)) == 'x'
                    && readLegacyHexColor(input, i, new StringBuilder(6)) == -1) {
                i++;
                continue;
            }

            cleaned.append(current);
        }

        return cleaned.toString();
    }

    private static int readLegacyHexColor(String input, int start, StringBuilder hex) {
        int index = start + 2;

        for (int i = 0; i < 6; i++) {
            if (index + 1 >= input.length() || !isLegacyColorPrefix(input.charAt(index))) {
                return -1;
            }

            char hexCharacter = input.charAt(index + 1);
            if (!isHexCharacter(hexCharacter)) {
                return -1;
            }

            hex.append(Character.toLowerCase(hexCharacter));
            index += 2;
        }

        return index;
    }

    private static boolean isLegacyColorPrefix(char character) {
        return character == '&' || character == LEGACY_SECTION_CHAR;
    }

    private static boolean isHexCharacter(char character) {
        return (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
    }



    public static String setColor(String s) {
        if (s == null) {
            return null;
        }

        String converted = normalizeLegacyHexColors(s).replaceAll("(?i)&#([a-f0-9]{6})", "<#$1>");
        converted = legacyToMiniMessage(converted);

        try {
            Component component = MiniMessage.miniMessage().deserialize(converted);

            if (MCVersion.isAtLeastVersion(MCVersion.MC1_16_R1)) {
                return HEX_LEGACY_SERIALIZER.serialize(component);
            }
            return LEGACY_SERIALIZER.serialize(component);
        } catch (Exception ignored) {
            return ChatColor.translateAlternateColorCodes('&', removeInvalidLegacyHexMarkers(s));
        }
    }





    public static boolean isInvFull(Player p) {
        return p.getInventory().firstEmpty() == -1;
    }


    public static boolean isDouble(String value) {
        if (value.equalsIgnoreCase("nan")) return false;
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException efr) {
            return false;
        }
        return true;
    }

    public static boolean isInt(String value) {
        if (value.equalsIgnoreCase("nan")) return false;
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
        if (MCVersion.isAtLeastVersion(MCVersion.MC1_12_R1)) return mat.isItem();
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

    public static boolean isMaterial(String s) {

        Material m;
        try {
            m = Material.matchMaterial(s);

        } catch (Exception e) {

            m = XMaterial.matchXMaterial(s).get().parseMaterial();
        }
        return m != null;

    }

    public void connect(Player p, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (Exception e) {
            e.printStackTrace();
        }

        p.sendPluginMessage(pl, "BungeeCord", out.toByteArray());
    }


    
    public static boolean isAxe(ItemStack item) {
        if (item == null) return false;
        if (item.getAmount() == 0) return false;
        return item.getType().name().endsWith("_AXE");
    }

    public static void repairItem(ItemStack stack) {
        Damageable damageableMeta = (Damageable) stack.getItemMeta();
        damageableMeta.setDamage(0);
        stack.setItemMeta(damageableMeta);
    }


    public static void addItem(Player p, ItemStack itemStack) {
        int stackAmount = itemStack.getAmount()/ 64;
        int stackRemainder = itemStack.getAmount()%64;
        itemStack.setAmount(64);

        for(int i = 0; i < stackAmount; i++){
            if(isInvFull(p)) p.getLocation().getWorld().dropItemNaturally(p.getLocation(),itemStack);
            else p.getInventory().addItem(itemStack.clone());
        }
        //Give stackRemainder
        itemStack.setAmount(stackRemainder);
        if(isInvFull(p)) p.getLocation().getWorld().dropItemNaturally(p.getLocation(),itemStack);
        else p.getInventory().addItem(itemStack);

    }
    public void playCustomSound(Player player, String soundId) {
        if (isValidSound(soundId)) {
            player.playSound(player.getLocation(), Sound.valueOf(soundId), 1.0F, 1.0F);
        } else {
            pl.getLogger().warning("Invalid sound ID: " + soundId);
        }
    }

    private static boolean isValidSound(String soundId) {
        for (Sound sound : Sound.values()) {
            if (sound.name().equalsIgnoreCase(soundId)) {
                return true;
            }
        }
        return false;
    }

    /*public static String formatStackSize(String message, int size){
        if (size <= 1 ) return message.replaceAll("%stack%", "");
        return message.replaceAll("%stack%",BeastLib.getInstance().getMessages().getConfig().getString("PlaceHolder.StackSize").replaceAll("%amount%",""+size));
    }*/

    public static int getTotalAmount(Inventory inventory, ItemStack target) {
        int amount = 0;

        for (ItemStack item : inventory) {
            if (item == null) continue;
            if (item.isSimilar(target)) amount += item.getAmount();
        }

        return amount;
    }

    public static String formatNumber(int number) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(number);
    }

    public void debug(@NotNull DebugLevel debugLevel, @NotNull Level level, @NotNull String... messages) {

        if (debugLevel().getPriority() > debugLevel.getPriority())
            return;
        pl.getLogger().log(level, String.join(System.lineSeparator(), messages));
    }

    public void printStacktrace(String message, Throwable throwable) {
        if (debugLevel().getPriority() <= DebugLevel.MEDIUM.getPriority())
            return;
        pl.getLogger().log(Level.SEVERE, message, throwable);
    }

    public static DebugLevel debugLevel() {
        return debugLevel;
    }


    public static String convertColorCodesToHex(String coloredString) {

        Pattern pattern = Pattern.compile("§x(§[0-9a-fA-F]){6}");
        Matcher matcher = pattern.matcher(coloredString);
        StringBuffer stringBuffer = new StringBuffer();

        while (matcher.find()) {
            String colorPattern = matcher.group();
            String hexColor =colorPattern.replaceAll("§x","&#").replaceAll("§","");
            matcher.appendReplacement(stringBuffer, hexColor);
        }

        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();

    }
    public static boolean isLeather(Material m) {

        return leather.contains(m);
    }


    public enum MessageType {
        CHAT,
        ACTION_BAR
    }






}
