package me.mraxetv.beastlib.api.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

/** Observes an interaction with an entity ID absent from the player's server world. */
public final class PlayerUnknownEntityUseEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final int entityId;
    private final boolean attack;
    private final EquipmentSlot hand;
    private final Vector clickedPosition;

    public PlayerUnknownEntityUseEvent(Player player, int entityId, boolean attack,
                                      EquipmentSlot hand, Vector clickedPosition) {
        super(player);
        this.entityId = entityId;
        this.attack = attack;
        this.hand = hand;
        this.clickedPosition = clickedPosition == null ? null : clickedPosition.clone();
    }

    public int getEntityId() { return entityId; }
    public boolean isAttack() { return attack; }
    public EquipmentSlot getHand() { return hand; }
    public Vector getClickedPosition() { return clickedPosition == null ? null : clickedPosition.clone(); }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
