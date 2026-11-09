package me.mraxetv.beastlib.api.items;

import org.bukkit.inventory.ItemStack;
import java.util.Collection;
import java.util.Optional;

/**
 * Obtain through Bukkit ServicesManager. Calls run on the server thread. Definitions and
 * collections are immutable detached snapshots; created stacks are fresh caller-owned items.
 * Identity comes from the owner's typed persisted data, not a matching name or model alone.
 */
public interface CustomItemService {
    Collection<ItemDefinition> definitions();
    Optional<ItemDefinition> definition(String id);
    Optional<ItemDefinition> identify(ItemStack item);
    /** Reject unknown/disabled definitions and amounts outside their legal stack capacity. */
    ItemStack create(String id, int amount);
}
