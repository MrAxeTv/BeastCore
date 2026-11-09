package me.mraxetv.beastlib.items;

import me.mraxetv.beastlib.api.items.ItemDefinition;
import me.mraxetv.beastlib.api.items.EnchantmentDefinition;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class ItemDefinitionsTest {
    @Test public void detachedDescriptorsCannotFollowLaterConfigMutations() {
        List<String> types = new ArrayList<String>(Arrays.asList("JEWELRY", "RING"));
        List<String> lore = new ArrayList<String>(Arrays.asList("&bLevel {level}"));
        ItemDefinition item = new ItemDefinition("beastrpgitems:moon_ring", "Moon Ring", "RING", types);
        EnchantmentDefinition enchant = new EnchantmentDefinition("beastenchants:lifesteal", "Life Steal", lore, 3, false);
        types.clear(); lore.clear();
        assertEquals(2, item.getTypes().size());
        assertEquals(Arrays.asList("&bLevel {level}"), enchant.getLore());
        try { item.getTypes().clear(); fail("types must be immutable"); }
        catch (UnsupportedOperationException expected) { }
        try { enchant.getLore().clear(); fail("lore must be immutable"); }
        catch (UnsupportedOperationException expected) { }
    }
    @Test public void malformedDefinitionsFailBeforePublication() {
        try { new EnchantmentDefinition("beastenchants:test", "Test", new ArrayList<String>(), 0, false); fail("zero max level"); }
        catch (IllegalArgumentException expected) { }
        try { new ItemDefinition("", "Test", "", Arrays.asList("ITEM")); fail("empty identity"); }
        catch (IllegalArgumentException expected) { }
        try { new EnchantmentDefinition("beastenchants:test", "Test", Arrays.asList((String) null), 1, false); fail("null lore"); }
        catch (NullPointerException expected) { }
    }
}
