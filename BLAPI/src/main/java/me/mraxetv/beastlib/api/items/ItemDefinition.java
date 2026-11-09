package me.mraxetv.beastlib.api.items;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Immutable detached description, safe to retain without a player, inventory or provider. */
public final class ItemDefinition {
    private final String id, displayName, equipmentSlot;
    private final Set<String> types;

    public ItemDefinition(String id, String displayName, String equipmentSlot, Collection<String> types) {
        this.id = required(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.equipmentSlot = Objects.requireNonNull(equipmentSlot, "equipmentSlot");
        LinkedHashSet<String> copy = new LinkedHashSet<String>();
        for (String type : Objects.requireNonNull(types, "types")) copy.add(required(type, "type"));
        this.types = Collections.unmodifiableSet(copy);
    }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    /** Empty for native/non-equipment items; RING can describe either anatomical ring address. */
    public String getEquipmentSlot() { return equipmentSlot; }
    public Set<String> getTypes() { return types; }
    private static String required(String value, String name) {
        if (Objects.requireNonNull(value, name).trim().isEmpty()) throw new IllegalArgumentException(name + " is empty");
        return value;
    }
}
