package me.mraxetv.beastlib.api.update;

import org.bukkit.plugin.java.JavaPlugin;

public final class UpdateServices {

    private static volatile UpdateService service = new NoOpUpdateService();

    private UpdateServices() {
    }

    public static UpdateService get() {
        return service;
    }

    public static void set(UpdateService updateService) {
        service = updateService == null ? new NoOpUpdateService() : updateService;
    }

    public static void clear() {
        service = new NoOpUpdateService();
    }

    private static final class NoOpUpdateService implements UpdateService {

        @Override
        public RegisteredUpdateChecker register(JavaPlugin plugin, String productKey) {
            return new NoOpRegisteredUpdateChecker(plugin, null);
        }

        @Override
        public RegisteredUpdateChecker register(JavaPlugin plugin, UpdateProductProfile profile) {
            return new NoOpRegisteredUpdateChecker(plugin, profile);
        }

        @Override
        public RegisteredUpdateChecker getRegistration(JavaPlugin plugin) {
            return new NoOpRegisteredUpdateChecker(plugin, null);
        }

        @Override
        public boolean isPremiumBuild(JavaPlugin plugin) {
            return false;
        }

        @Override
        public boolean isFeatureAvailable(JavaPlugin plugin, String featureKey) {
            return false;
        }

        @Override
        public void unregister(JavaPlugin plugin) {
        }

        @Override
        public UpdateProductProfile getProductProfile(String productKey) {
            return null;
        }
    }

    private static final class NoOpRegisteredUpdateChecker implements RegisteredUpdateChecker {
        private final JavaPlugin plugin;
        private final UpdateProductProfile profile;

        private NoOpRegisteredUpdateChecker(JavaPlugin plugin, UpdateProductProfile profile) {
            this.plugin = plugin;
            this.profile = profile;
        }

        @Override
        public JavaPlugin getPlugin() {
            return plugin;
        }

        @Override
        public UpdateProductProfile getProfile() {
            return profile;
        }

        @Override
        public UpdateBuildChannel getBuildChannel() {
            return UpdateBuildChannel.UNKNOWN;
        }

        @Override
        public boolean isPremiumBuild() {
            return false;
        }

        @Override
        public boolean isFeatureAvailable(String featureKey) {
            return profile != null && !profile.isPremiumFeature(featureKey);
        }

        @Override
        public boolean isFreeUpdateAvailable() {
            return false;
        }

        @Override
        public boolean isPremiumUpdateAvailable() {
            return false;
        }

        @Override
        public String getLatestFreeVersion() {
            return null;
        }

        @Override
        public String getLatestPremiumVersion() {
            return null;
        }

        @Override
        public void checkNow() {
        }

        @Override
        public void shutdown() {
        }
    }
}
