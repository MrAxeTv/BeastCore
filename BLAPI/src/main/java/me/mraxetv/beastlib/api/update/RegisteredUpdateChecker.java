package me.mraxetv.beastlib.api.update;

import org.bukkit.plugin.java.JavaPlugin;

public interface RegisteredUpdateChecker {

    JavaPlugin getPlugin();

    UpdateProductProfile getProfile();

    UpdateBuildChannel getBuildChannel();

    boolean isPremiumBuild();

    boolean isFeatureAvailable(String featureKey);

    boolean isFreeUpdateAvailable();

    boolean isPremiumUpdateAvailable();

    String getLatestFreeVersion();

    String getLatestPremiumVersion();

    void checkNow();

    void shutdown();
}
