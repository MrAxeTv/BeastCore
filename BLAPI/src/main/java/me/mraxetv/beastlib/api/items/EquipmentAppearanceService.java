package me.mraxetv.beastlib.api.items;

import org.bukkit.inventory.ItemStack;
import java.util.Collection;
import java.util.Optional;

/**
 * Optional server-thread factory for the equipment owner's genuine visual templates.
 * Consumers can decorate the returned real item with their own identity, attributes and
 * enchantments while retaining its original model and container metadata. This service does
 * not equip items, grant provider gameplay effects, combine packs or offer a resource pack.
 */
public interface EquipmentAppearanceService {
    /** Immutable detached descriptions; RING identifies either anatomical ring address. */
    Collection<ItemDefinition> templates();

    /**
     * A fresh, single caller-owned real item, or empty for an unknown/unavailable template.
     * Retain the owner's material, model and identity data. Never copy identity keys by hand;
     * container templates must receive their own unique identity on every creation.
     */
    Optional<ItemStack> create(String id);
}
