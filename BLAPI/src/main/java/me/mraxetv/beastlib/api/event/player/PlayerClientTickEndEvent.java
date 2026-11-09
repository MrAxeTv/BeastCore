package me.mraxetv.beastlib.api.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/** One completed native client input frame, delivered on the player/server thread. */
public final class PlayerClientTickEndEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public PlayerClientTickEndEvent(Player player) { super(player); }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
