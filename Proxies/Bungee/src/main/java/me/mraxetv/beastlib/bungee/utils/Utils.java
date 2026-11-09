package me.mraxetv.beastlib.bungee.utils;

import me.mraxetv.beastlib.bungee.BeastLib;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;

public class Utils<T extends Plugin> extends PUtils{

    private BeastLib pl;
    private static String prefix;
    public Utils(BeastLib pl) {
        super(pl);
    }


    public String getPrefix(){
        return prefix;
    }

    public void sendMessage(CommandSender sender, String s) {
        s = s.replaceAll("%prefix%", getPrefix());
        s = setColor(s);
        sender.sendMessage(new TextComponent(s));
    }

    public void sendLog(String s) {
        s = ChatColor.translateAlternateColorCodes('&', s);
        BeastLib.getInstance().getProxy().getConsole().sendMessage(getPrefix() + " " + s);
    }

    //public static void warningLog(String message) {
    //    pl.getProxy().getLogger().warning(ANSI_RED + "[" + pl.getDescription().getName() + "] " + message + ANSI_RESET);
    //}

    public static String setColor(String s) {
        s = setHexColors(s);
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
