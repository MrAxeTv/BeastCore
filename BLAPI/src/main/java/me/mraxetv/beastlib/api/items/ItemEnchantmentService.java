package me.mraxetv.beastlib.api.items;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

/**
 * Server-thread enchantment extension from Bukkit ServicesManager. Reads return immutable
 * detached snapshots. Apply/remove return a modified clone without changing their argument;
 * preserve unrelated metadata, native enchantments, custom identities and user lore.
 * Implementations reject unknown/disabled IDs, invalid levels and incompatible applications.
 */
public interface ItemEnchantmentService {
    Collection<EnchantmentDefinition> definitions();
    Optional<EnchantmentDefinition> describe(String id);
    Map<String, Integer> enchantments(ItemStack item);
    ItemStack apply(ItemStack item, String id, int level);
    ItemStack remove(ItemStack item, String id);
    /** Rendered provider-owned lines only; this is not the item's complete user lore. */
    List<String> lore(ItemStack item);

    /**
     * Immutable base/user lore after removing only this provider's verified managed span.
     * A changed or unrecognizable managed span must fail safely, never be removed by text search.
     * Legacy providers without managed lore can use this default implementation.
     */
    default List<String> baseLore(ItemStack item) {
        ItemStack copy = Objects.requireNonNull(item, "item").clone();
        if (!lore(copy).isEmpty()) {
            throw new UnsupportedOperationException("Managed lore provider must implement baseLore");
        }
        ItemMeta meta = copy.getItemMeta();
        List<String> lines = meta == null ? null : meta.getLore();
        return lines == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(lines));
    }

    /**
     * Replace the caller-owned base contribution on a clone, then reapply this provider's
     * managed lines and receipt. Preserve enchantment levels, identities, unrelated metadata
     * and ownership of hidden-enchantment flags. The argument remains unchanged.
     */
    default ItemStack withBaseLore(ItemStack item, List<String> baseLore) {
        ItemStack copy = Objects.requireNonNull(item, "item").clone();
        Objects.requireNonNull(baseLore, "baseLore");
        if (!lore(copy).isEmpty()) {
            throw new UnsupportedOperationException("Managed lore provider must implement withBaseLore");
        }
        List<String> lines = new ArrayList<>(baseLore);
        for (String line : lines) Objects.requireNonNull(line, "lore line");
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) throw new IllegalArgumentException("Item has no metadata");
        meta.setLore(lines);
        if (!copy.setItemMeta(meta)) throw new IllegalArgumentException("Item rejected metadata");
        return copy;
    }
}
