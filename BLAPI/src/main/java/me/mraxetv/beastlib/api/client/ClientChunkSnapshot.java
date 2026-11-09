package me.mraxetv.beastlib.api.client;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable receipt of chunks sent to one connection in one world. An empty
 * snapshot is returned when authoritative pre-watch history is unavailable;
 * loaded server chunks are never used as a substitute for client receipts.
 */
public final class ClientChunkSnapshot {
    private final UUID worldId;
    private final Set<Long> chunkKeys;

    public ClientChunkSnapshot(UUID worldId, Collection<Long> chunkKeys) {
        this.worldId = Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(chunkKeys, "chunkKeys");
        LinkedHashSet<Long> copy = new LinkedHashSet<Long>();
        for (Long key : chunkKeys) copy.add(Objects.requireNonNull(key, "chunk key"));
        this.chunkKeys = Collections.unmodifiableSet(copy);
    }

    public UUID getWorldId() { return worldId; }
    public Set<Long> getChunkKeys() { return chunkKeys; }
    public boolean isEmpty() { return chunkKeys.isEmpty(); }

    /** X occupies the low 32 bits and Z occupies the high 32 bits. */
    public static long chunkKey(int chunkX, int chunkZ) {
        return (chunkX & 0xffffffffL) | ((long) chunkZ << 32);
    }
}
