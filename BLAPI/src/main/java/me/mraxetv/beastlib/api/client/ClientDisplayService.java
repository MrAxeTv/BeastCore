package me.mraxetv.beastlib.api.client;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Sends Minecraft JSON components without exposing a particular component library.
 * Callers must use the thread/context that owns the receiving player or entity.
 * Features unavailable on an older server fail when called, not during enable.
 */
public interface ClientDisplayService {
    void sendMessage(CommandSender receiver, String json);
    void sendActionBar(Player player, String json);
    void sendTitle(Player player, String json);
    void sendSubtitle(Player player, String json);
    void setTitleTimes(Player player, int fadeIn, int stay, int fadeOut);
    void clearTitle(Player player, boolean reset);
    void setDisplayText(Entity entity, String json);
    void showDialog(Player player, String json);
    void closeDialog(Player player);
}
