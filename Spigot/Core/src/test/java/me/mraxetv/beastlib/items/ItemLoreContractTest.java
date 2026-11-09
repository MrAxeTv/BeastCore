package me.mraxetv.beastlib.items;

import me.mraxetv.beastlib.api.items.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.Assert.*;

public class ItemLoreContractTest {
    @Test public void legacyProviderDefaultsDoNotDescribeUnownedItems() {
        EquipmentItemProvider provider = new EquipmentItemProvider() {
            public String namespace() { return "legacy"; }
            public boolean accepts(Player player, String slot, ItemStack item) { return false; }
        };
        assertFalse(provider.describe(new Stack(Arrays.asList("user"))).isPresent());
    }

    @Test public void unmanagedLoreReplacementIsDetachedAndPreservesOtherMetadata() {
        ItemEnchantmentService service = service(Collections.emptyList());
        Stack source = new Stack(Arrays.asList("original"));
        List<String> read = service.baseLore(source);
        try { read.clear(); fail("immutable base lore"); }
        catch (UnsupportedOperationException expected) { }
        List<String> replacement = new ArrayList<>(Arrays.asList("changed", "tail"));
        ItemStack result = service.withBaseLore(source, replacement);
        replacement.clear();
        assertEquals(Arrays.asList("original"), source.getItemMeta().getLore());
        assertEquals(Arrays.asList("changed", "tail"), result.getItemMeta().getLore());
        assertEquals("foreign name", result.getItemMeta().getDisplayName());
        assertNotSame(source, result);
    }

    @Test public void legacyManagedLoreCannotBeRebasedWithoutAnOverride() {
        ItemEnchantmentService service = service(Arrays.asList("managed"));
        Stack source = new Stack(Arrays.asList("user", "managed"));
        try { service.baseLore(source); fail("managed provider must override"); }
        catch (UnsupportedOperationException expected) { }
        try { service.withBaseLore(source, Arrays.asList("replacement")); fail("must reject"); }
        catch (UnsupportedOperationException expected) { }
        assertEquals(Arrays.asList("user", "managed"), source.getItemMeta().getLore());
    }

    private static ItemEnchantmentService service(final List<String> managed) {
        return new ItemEnchantmentService() {
            public Collection<EnchantmentDefinition> definitions() { return Collections.emptyList(); }
            public Optional<EnchantmentDefinition> describe(String id) { return Optional.empty(); }
            public Map<String, Integer> enchantments(ItemStack item) { return Collections.emptyMap(); }
            public ItemStack apply(ItemStack item, String id, int level) { throw new UnsupportedOperationException(); }
            public ItemStack remove(ItemStack item, String id) { throw new UnsupportedOperationException(); }
            public List<String> lore(ItemStack item) { return managed; }
        };
    }

    private static final class Stack extends ItemStack {
        private List<String> lines;
        private Stack(List<String> lines) { super(Material.PAPER); this.lines = new ArrayList<>(lines); }
        @Override public Stack clone() { return new Stack(lines); }
        @Override public ItemMeta getItemMeta() {
            final List<String>[] detached = new List[] { new ArrayList<>(lines) };
            return (ItemMeta) Proxy.newProxyInstance(ItemMeta.class.getClassLoader(), new Class<?>[] { ItemMeta.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLore": return new ArrayList<>(detached[0]);
                        case "setLore": detached[0] = new ArrayList<>((List<String>) args[0]); return null;
                        case "getDisplayName": return "foreign name";
                        case "hasLore": return !detached[0].isEmpty();
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
        }
        @Override public boolean setItemMeta(ItemMeta meta) { lines = new ArrayList<>(meta.getLore()); return true; }
    }
}
