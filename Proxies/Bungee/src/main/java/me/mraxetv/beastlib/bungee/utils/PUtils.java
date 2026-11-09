package me.mraxetv.beastlib.bungee.utils;

import me.mraxetv.beastlib.bungee.BeastLib;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PUtils {

    private BeastLib pl;
    private final static Pattern PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");


    public PUtils(BeastLib pl) {

        this.pl = pl;
    }



    public static String setHexColors(String s) {

        Matcher matcher = PATTERN.matcher(s);
        while (matcher.find()) {
            String c = s.substring(matcher.start(), matcher.end());

            s = s.replace(c, "" + ChatColor.of(c.replace("&", "")));
            matcher = PATTERN.matcher(s);
        }
        return s;
    }

   /* public static boolean isDecimalsAllowed() {

        return decimalsAmountB;
    }
    public static String formatNumber(double number) {

        return tokenFormatter.format(number);
    }*/

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

  /*  public void noPermission(CommandSender p) {
        String s = ChatColor.translateAlternateColorCodes('&', pl.getMessages().getString("Player.NoPermission"));
        s = s.replaceAll("%prefix%", getPrefix());
        p.sendMessage(s);
    }*/



}
