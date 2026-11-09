package me.mraxetv.beastlib.api.client;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Server-side observations of real client packets and chunk sends. Subscribe
 * to the neutral Bukkit events in api.event.player after calling watch.
 * Watch and snapshot reads run on the player/server thread. Network decoding
 * never calls plugin listeners directly. This API does not claim Folia support.
 */
public interface ClientEventService {
    default ClientEventRegistration watch(Plugin owner) {
        return watch(owner, player -> { });
    }

    /**
     * Starts observation for an enabled owner. A failed connection stream is
     * stopped, and failureCallback runs once on its player/server thread; the
     * owner should stop interactions which depend on complete input frames.
     * Unsupported native bindings fail synchronously instead of inventing ticks.
     * After the first watch, BeastLib retains bounded connection chunk receipts
     * until quit/shutdown, including gaps with no owners, so consumer reloads
     * cannot lose actual-send history. Closing removes the owner's callbacks.
     */
    ClientEventRegistration watch(Plugin owner, Consumer<Player> failureCallback);

    /**
     * Returns immutable, current-world sent-chunk keys for this exact Player
     * connection. Without authoritative bootstrap, only sends observed after
     * watch are included. This does not mean the client rendered those chunks.
     */
    ClientChunkSnapshot sentChunks(Player player);

    /** Closes every registration held by owner. Safe to invoke more than once. */
    void close(Plugin owner);

    boolean isShutdown();
}
