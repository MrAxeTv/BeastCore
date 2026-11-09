package me.mraxetv.beastlib.api.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable configured enchantment presentation, independent of Bukkit registration. */
public final class EnchantmentDefinition {
    private final String id, displayName;
    private final List<String> lore;
    private final int maxLevel;
    private final boolean vanilla;

    public EnchantmentDefinition(String id, String displayName, List<String> lore, int maxLevel, boolean vanilla) {
        this.id = Objects.requireNonNull(id, "id");
        if (id.trim().isEmpty()) throw new IllegalArgumentException("id is empty");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        ArrayList<String> copy = new ArrayList<String>();
        for (String line : Objects.requireNonNull(lore, "lore")) copy.add(Objects.requireNonNull(line, "lore line"));
        this.lore = Collections.unmodifiableList(copy);
        if (maxLevel < 1) throw new IllegalArgumentException("maxLevel must be positive");
        this.maxLevel = maxLevel;
        this.vanilla = vanilla;
    }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    /** Configured templates; providers document their supported substitutions. */
    public List<String> getLore() { return lore; }
    public int getMaxLevel() { return maxLevel; }
    public boolean isVanilla() { return vanilla; }
}
