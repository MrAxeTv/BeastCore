package me.mraxetv.beastlib.api.items;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Server-thread ownership boundary for genuinely equipped custom items. Obtain through
 * Bukkit ServicesManager; an installed but unavailable equipment owner must fail closed.
 * Slot names retain the owner's public aliases. Reads return detached snapshots, never GUI
 * controls. Transfers preserve metadata, curses, capacity, rollback and death policy.
 */
public interface EquipmentService {
    /** Detached real item, or null for an empty/unknown/unavailable address. */
    ItemStack item(Player player, String slot);

    /** Advisory validation; a later transfer rechecks permissions, lifecycle and ownership. */
    boolean accepts(Player player, String slot, ItemStack item);

    /**
     * Replace an existing exact snapshot; null removes it. Cannot insert into an empty address.
     * Intended for provider metadata/charge updates, independently of GUI editing permission.
     * False means no mutation. Implementations must not mutate expected or replacement.
     */
    boolean compareAndSet(Player player, String slot, ItemStack expected, ItemStack replacement);

    /**
     * Atomically move one exact item from native storage 0..35 into equipment, returning any
     * displaced item to owned storage. Recheck editing, source, capacity and curses. No drops,
     * mirrors or automatic adoption. False means no mutation; expected remains unchanged.
     */
    boolean equipFromStorage(Player player, String slot, int sourceIndex, ItemStack expected);

    /** Single authoritative bracelet address; older/custom layouts can return GLOVES. */
    default String braceletSlot() { return "BRACELET"; }

    /** Optional exact-byte migration of an already equipped, provider-verified legacy cuff. */
    default boolean migrateLegacyBracelet(Player player) { return false; }
}
