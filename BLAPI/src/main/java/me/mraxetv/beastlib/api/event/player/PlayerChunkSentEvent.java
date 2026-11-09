package me.mraxetv.beastlib.api.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import java.util.Objects;
import java.util.UUID;

/** A real outbound chunk receipt; accessing this event never loads a chunk. */
public final class PlayerChunkSentEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID worldId;
    private final int chunkX;
    private final int chunkZ;

    public PlayerChunkSentEvent(Player player, UUID worldId, int chunkX, int chunkZ) {
        super(player);
        this.worldId = Objects.requireNonNull(worldId, "worldId");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public UUID getWorldId() { return worldId; }
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
