package me.mraxetv.beastlib.api.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/** A client custom-click response, including actions submitted by a dialog. */
public final class PlayerDialogResponseEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String actionId;
    private final String payloadJson;

    public PlayerDialogResponseEvent(Player player, String actionId, String payloadJson) {
        super(player);
        if (actionId == null) throw new IllegalArgumentException("actionId cannot be null");
        this.actionId = actionId;
        this.payloadJson = payloadJson == null ? "null" : payloadJson;
    }

    public String getActionId() { return actionId; }
    public String getPayload() { return payloadJson; }
    public String getPayloadJson() { return payloadJson; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
