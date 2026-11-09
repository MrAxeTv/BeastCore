package me.mraxetv.beastlib.api.update;

import org.bukkit.plugin.java.JavaPlugin;

public interface UpdateService {

    RegisteredUpdateChecker register(JavaPlugin plugin, String productKey);

    RegisteredUpdateChecker register(JavaPlugin plugin, UpdateProductProfile profile);

    RegisteredUpdateChecker getRegistration(JavaPlugin plugin);

    boolean isPremiumBuild(JavaPlugin plugin);

    boolean isFeatureAvailable(JavaPlugin plugin, String featureKey);

    void unregister(JavaPlugin plugin);

    UpdateProductProfile getProductProfile(String productKey);
}
