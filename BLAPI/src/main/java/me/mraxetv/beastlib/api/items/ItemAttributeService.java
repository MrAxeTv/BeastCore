package me.mraxetv.beastlib.api.items;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

/**
 * Optional server-thread numeric custom-stat descriptions. Keys are namespace-qualified;
 * values are finite and collections are immutable detached snapshots. Reading these values
 * does not install Bukkit modifiers or execute combat effects: their consumer owns gameplay.
 */
public interface ItemAttributeService {
    /** Custom stats persisted on a genuine provider item; unknown/invalid items return empty. */
    Map<String, Double> attributes(ItemStack item);

    /**
     * Aggregate this provider's genuinely equipped items, including supported native addresses.
     * Never adopt inventory storage, cursor items, GUI carriers or an unavailable RPG authority.
     */
    Map<String, Double> equipped(Player player);
}
