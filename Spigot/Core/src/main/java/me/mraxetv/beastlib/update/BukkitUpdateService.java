package me.mraxetv.beastlib.update;

import me.mraxetv.beastlib.BeastLib;
import me.mraxetv.beastlib.api.update.RegisteredUpdateChecker;
import me.mraxetv.beastlib.api.update.UpdateProductProfile;
import me.mraxetv.beastlib.api.update.UpdateService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BukkitUpdateService implements UpdateService, Listener {

    private final BeastLib beastLib;
    private final Map<String, UpdateProductProfile> productProfiles = new ConcurrentHashMap<>();
    private final Map<JavaPlugin, BukkitRegisteredUpdateChecker> registrations = new ConcurrentHashMap<>();
    private boolean listenerRegistered;

    public BukkitUpdateService(BeastLib beastLib) {
        this.beastLib = beastLib;
        registerProductProfile(beastWithdrawProfile());
    }

    public void start() {
        if (!listenerRegistered) {
            beastLib.getServer().getPluginManager().registerEvents(this, beastLib);
            listenerRegistered = true;
        }

        for (Plugin plugin : beastLib.getServer().getPluginManager().getPlugins()) {
            autoRegister(plugin);
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        autoRegister(event.getPlugin());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Plugin plugin = event.getPlugin();
        if (plugin instanceof JavaPlugin) {
            unregister((JavaPlugin) plugin);
        }
    }

    public void registerProductProfile(UpdateProductProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile cannot be null");
        }
        productProfiles.put(normalizeKey(profile.getProductKey()), profile);
    }

    @Override
    public RegisteredUpdateChecker register(JavaPlugin plugin, String productKey) {
        UpdateProductProfile profile = getProductProfile(productKey);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown update product: " + productKey);
        }
        return register(plugin, profile);
    }

    @Override
    public RegisteredUpdateChecker register(JavaPlugin plugin, UpdateProductProfile profile) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        if (profile == null) {
            throw new IllegalArgumentException("profile cannot be null");
        }

        unregister(plugin);
        BukkitRegisteredUpdateChecker checker = new BukkitRegisteredUpdateChecker(beastLib, plugin, profile);
        registrations.put(plugin, checker);
        checker.start();
        return checker;
    }

    @Override
    public RegisteredUpdateChecker getRegistration(JavaPlugin plugin) {
        if (plugin == null) {
            return null;
        }
        autoRegister(plugin);
        return registrations.get(plugin);
    }

    @Override
    public boolean isPremiumBuild(JavaPlugin plugin) {
        RegisteredUpdateChecker checker = getRegistration(plugin);
        return checker != null && checker.isPremiumBuild();
    }

    @Override
    public boolean isFeatureAvailable(JavaPlugin plugin, String featureKey) {
        RegisteredUpdateChecker checker = getRegistration(plugin);
        return checker != null && checker.isFeatureAvailable(featureKey);
    }

    @Override
    public void unregister(JavaPlugin plugin) {
        if (plugin == null) {
            return;
        }
        BukkitRegisteredUpdateChecker checker = registrations.remove(plugin);
        if (checker != null) {
            checker.shutdown();
        }
    }

    @Override
    public UpdateProductProfile getProductProfile(String productKey) {
        String normalized = normalizeKey(productKey);
        if (normalized == null) {
            return null;
        }
        return productProfiles.get(normalized);
    }

    public void shutdown() {
        Collection<BukkitRegisteredUpdateChecker> checkers = registrations.values();
        for (BukkitRegisteredUpdateChecker checker : checkers) {
            checker.shutdown();
        }
        registrations.clear();

        if (listenerRegistered) {
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
        }
    }

    private void autoRegister(Plugin plugin) {
        if (!(plugin instanceof JavaPlugin)) {
            return;
        }

        JavaPlugin javaPlugin = (JavaPlugin) plugin;
        if (!javaPlugin.isEnabled()) {
            return;
        }
        if (registrations.containsKey(javaPlugin)) {
            return;
        }

        UpdateProductProfile profile = findProfile(javaPlugin);
        if (profile != null) {
            register(javaPlugin, profile);
        }
    }

    private UpdateProductProfile findProfile(JavaPlugin plugin) {
        String pluginName = normalizeKey(plugin.getName());
        if (pluginName == null) {
            return null;
        }

        for (UpdateProductProfile profile : productProfiles.values()) {
            if (pluginName.equals(normalizeKey(profile.getProductKey()))
                    || pluginName.equals(normalizeKey(profile.getDisplayName()))) {
                return profile;
            }
        }
        return null;
    }

    private static UpdateProductProfile beastWithdrawProfile() {
        return UpdateProductProfile.builder("beastwithdraw")
                .displayName("BeastWithdraw")
                .messagePrefix("&7[&4Beast&bWithdraw&7]")
                .freeProject("beastwithdraw")
                .freeDownloadUrl("https://modrinth.com/plugin/beastwithdraw")
                .freeDisplayName("FREE")
                .premiumResource(130867)
                .premiumDownloadUrl("https://www.spigotmc.org/resources/beastwithdraw-plus-multy-withdraw.130867/")
                .premiumDisplayName("BeastWithdrawPlus")
                .premiumFeatures(
                        "withdraw_gui", // Plus version feature
                        "deposit_gui", // Plus version feature
                        "beastlifesteal", // Plus version feature
                        "beastmcmmo_redeem", // Plus version feature
                        "beastmcmmo_redeem_skill", // Plus version feature
                        "playerpoints", // Plus version feature
                        "custom_model_data", // Plus version feature
                        "amount_item_overrides", // Plus version feature
                        "skull_textures", // Plus version feature
                        "required_base_item", // Plus version feature
                        "gui_animations", // Plus version feature
                        "gui_sounds") // Plus version feature
                .adminPermission("BeastWithdraw.Admin")
                .adminJoinConfigPath("Settings.UpdateNotifications.AdminJoin.Enabled")
                .premiumMarkerFieldName("x7")
                .build();
    }

    private static String normalizeKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ENGLISH);
    }
}
