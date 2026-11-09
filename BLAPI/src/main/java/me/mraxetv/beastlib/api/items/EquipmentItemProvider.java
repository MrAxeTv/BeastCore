package me.mraxetv.beastlib.api.items;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Optional;

/**
 * Optional server-thread validation supplied by an item owner. Register with Bukkit
 * ServicesManager; equipment owners query all enabled registrations, not only the highest
 * priority. Validate typed persistent identity, active definitions and permission, never
 * display names/lore alone. No mutation, transfer or call back into EquipmentService.accepts.
 */
public interface EquipmentItemProvider {
    /** Stable lowercase item-owner namespace, for example beastvampires. */
    String namespace();
    boolean accepts(Player player, String slot, ItemStack item);

    /**
     * Describe genuine typed identity without checking player permission or equipment state.
     * This does not grant an item, equip it, or consume charges. Unknown, inactive or malformed
     * items return empty. Callers pass a detached stack and revalidate the enabled owner.
     */
    default Optional<ItemDefinition> describe(ItemStack item) {
        return Optional.empty();
    }
}
