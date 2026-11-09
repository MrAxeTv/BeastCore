package me.mraxetv.beastlib.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.mraxetv.beastlib.BeastLib;
import me.mraxetv.beastlib.api.update.RegisteredUpdateChecker;
import me.mraxetv.beastlib.api.update.UpdateBuildChannel;
import me.mraxetv.beastlib.api.update.UpdateProductProfile;
import me.mraxetv.beastlib.api.scheduler.SchedulerTask;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class BukkitRegisteredUpdateChecker implements RegisteredUpdateChecker, Listener {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    private static final String RED = "&4&l";
    private static final String BLUE = "&b&l";
    private static final String D = "&8";
    private static final String G = "&7";
    private static final String W = "&f";
    private static final String OK = "&a";
    private static final String BAD = "&c";
    private static final String Y = "&e";

    private final BeastLib beastLib;
    private final JavaPlugin plugin;
    private final UpdateProductProfile profile;
    private final VersionState freeState = new VersionState();
    private final VersionState premiumState = new VersionState();
    private final List<SchedulerTask> tasks = new ArrayList<>();
    private final Map<UUID, Long> joinCooldown = new HashMap<>();

    private volatile UpdateBuildChannel buildChannel = UpdateBuildChannel.UNKNOWN;
    private volatile boolean shutdown;
    private boolean listenerRegistered;

    BukkitRegisteredUpdateChecker(BeastLib beastLib, JavaPlugin plugin, UpdateProductProfile profile) {
        this.beastLib = beastLib;
        this.plugin = plugin;
        this.profile = profile;
    }

    void start() {
        buildChannel = detectBuildChannel();

        if (buildChannel == UpdateBuildChannel.INVALID) {
            sendInvalidBuildWarning();
            return;
        }

        if (buildChannel == UpdateBuildChannel.PREMIUM) {
            startPremiumChecks();
        } else if (buildChannel == UpdateBuildChannel.FREE) {
            startFreeChecks();
        } else {
            startUnknownChecks();
        }

        plugin.getServer().getPluginManager().registerEvents(this, beastLib);
        listenerRegistered = true;
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
        return buildChannel;
    }

    @Override
    public boolean isPremiumBuild() {
        return buildChannel == UpdateBuildChannel.PREMIUM;
    }

    @Override
    public boolean isFeatureAvailable(String featureKey) {
        if (!profile.isPremiumFeature(featureKey)) {
            return true;
        }
        return buildChannel == UpdateBuildChannel.PREMIUM;
    }

    @Override
    public boolean isFreeUpdateAvailable() {
        return freeState.updateAvailable;
    }

    @Override
    public boolean isPremiumUpdateAvailable() {
        return premiumState.updateAvailable;
    }

    @Override
    public String getLatestFreeVersion() {
        return freeState.latestVersion;
    }

    @Override
    public String getLatestPremiumVersion() {
        return premiumState.latestVersion;
    }

    @Override
    public void checkNow() {
        if (shutdown || !plugin.isEnabled() || buildChannel == UpdateBuildChannel.INVALID) {
            return;
        }

        beastLib.getScheduler().runAsync(plugin, new Runnable() {
            @Override
            public void run() {
                if (shutdown) {
                    return;
                }
                checkChannels();
            }
        });
    }

    @Override
    public void shutdown() {
        shutdown = true;

        for (SchedulerTask task : tasks) {
            task.cancel();
        }
        tasks.clear();

        if (listenerRegistered) {
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
        }
        joinCooldown.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!isAdminJoinEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        String permission = profile.getAdminPermission();
        if (permission != null && !player.hasPermission(permission)) {
            return;
        }
        if (permission == null && !player.isOp()) {
            return;
        }
        if (isOnCooldown(player)) {
            return;
        }

        sendJoinMessage(player);
    }

    private void startFreeChecks() {
        addTask(beastLib.getScheduler().runGlobalDelayed(plugin, new Runnable() {
            @Override
            public void run() {
                if (shutdown) {
                    return;
                }
                beastLib.getScheduler().runAsync(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (shutdown) {
                            return;
                        }
                        checkFree();
                        checkPremium();
                        beastLib.getScheduler().runGlobal(plugin, new Runnable() {
                            @Override
                            public void run() {
                                if (!shutdown) {
                                    sendStartupConsoleMessage();
                                }
                            }
                        });
                    }
                });
            }
        }, profile.getFreeStartupMessageDelayTicks()));

        addTask(beastLib.getScheduler().runGlobalRepeating(plugin, new Runnable() {
            @Override
            public void run() {
                checkNow();
            }
        }, profile.getRecheckTicks(), profile.getRecheckTicks()));
    }

    private void startPremiumChecks() {
        checkNow();

        addTask(beastLib.getScheduler().runGlobalRepeating(plugin, new Runnable() {
            @Override
            public void run() {
                checkNow();
            }
        }, profile.getRecheckTicks(), profile.getRecheckTicks()));

        addTask(beastLib.getScheduler().runGlobalDelayed(plugin, new Runnable() {
            @Override
            public void run() {
                sendStartupConsoleMessage();
            }
        }, profile.getStartupDelayTicks()));
    }

    private void startUnknownChecks() {
        checkNow();

        addTask(beastLib.getScheduler().runGlobalRepeating(plugin, new Runnable() {
            @Override
            public void run() {
                checkNow();
            }
        }, profile.getRecheckTicks(), profile.getRecheckTicks()));

        addTask(beastLib.getScheduler().runGlobalDelayed(plugin, new Runnable() {
            @Override
            public void run() {
                sendStartupConsoleMessage();
            }
        }, profile.getStartupDelayTicks()));
    }

    private void checkChannels() {
        if (buildChannel == UpdateBuildChannel.PREMIUM) {
            checkPremium();
            return;
        }

        if (buildChannel == UpdateBuildChannel.FREE) {
            checkFree();
            checkPremium();
            return;
        }

        checkFree();
        checkPremium();
    }

    private void checkFree() {
        if (!profile.hasFreeChannel()) {
            return;
        }

        try {
            String url = "https://api.modrinth.com/v2/project/" + profile.getFreeProjectSlugOrId() + "/version";
            String responseBody = get(url, userAgent());
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return;
            }

            JsonArray versions = new JsonParser().parse(responseBody).getAsJsonArray();
            if (versions.size() == 0) {
                return;
            }

            JsonObject latest = null;
            for (JsonElement element : versions) {
                JsonObject candidate = element.getAsJsonObject();
                if (latest == null || isPublishedAfter(candidate, latest)) {
                    latest = candidate;
                }
            }

            if (latest == null || !latest.has("version_number")) {
                return;
            }

            String newest = latest.get("version_number").getAsString();
            updateState(freeState, newest);
        } catch (Exception ignored) {
        }
    }

    private void checkPremium() {
        if (!profile.hasPremiumChannel()) {
            return;
        }

        try {
            String url = "https://api.spigotmc.org/legacy/update.php?resource=" + profile.getPremiumResourceId();
            String responseBody = get(url, userAgent());
            if (responseBody == null) {
                return;
            }

            String newest = responseBody.trim();
            if (newest.isEmpty()) {
                return;
            }

            updateState(premiumState, newest);
        } catch (Exception ignored) {
        }
    }

    private void updateState(VersionState state, String newest) {
        state.latestVersion = newest;
        state.updateAvailable = isNewer(newest, plugin.getDescription().getVersion());
    }

    private boolean isPublishedAfter(JsonObject candidate, JsonObject latest) {
        try {
            Instant latestInstant = Instant.parse(latest.get("date_published").getAsString());
            Instant candidateInstant = Instant.parse(candidate.get("date_published").getAsString());
            return candidateInstant.isAfter(latestInstant);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void sendStartupConsoleMessage() {
        if (shutdown) {
            return;
        }

        CommandSender console = plugin.getServer().getConsoleSender();
        if (buildChannel == UpdateBuildChannel.PREMIUM) {
            if (premiumState.updateAvailable) {
                sendPremiumUpdate(console);
            }
            return;
        }

        if (buildChannel == UpdateBuildChannel.FREE) {
            if (freeState.updateAvailable) {
                sendFreeCombined(console);
                return;
            }
            if (premiumState.updateAvailable) {
                sendPremiumPromo(console);
            }
            return;
        }

        if (freeState.updateAvailable) {
            sendFreeCombined(console);
        } else if (premiumState.updateAvailable) {
            sendPremiumUpdate(console);
        }
    }

    private void sendJoinMessage(Player player) {
        if (buildChannel == UpdateBuildChannel.PREMIUM) {
            if (premiumState.updateAvailable) {
                sendPremiumUpdate(player);
            }
            return;
        }

        if (buildChannel == UpdateBuildChannel.FREE) {
            if (freeState.updateAvailable) {
                sendFreeCombined(player);
                return;
            }
            if (premiumState.updateAvailable) {
                sendPremiumPromo(player);
            }
            return;
        }

        if (freeState.updateAvailable) {
            sendFreeCombined(player);
        } else if (premiumState.updateAvailable) {
            sendPremiumUpdate(player);
        }
    }

    private void sendFreeCombined(CommandSender sender) {
        if (freeState.latestVersion == null) {
            return;
        }

        send(sender, border());
        send(sender, title(RED + "FREE UPDATE AVAILABLE"));
        send(sender, "%prefix% " + G + "A newer free build is available on " + BLUE + "Modrinth" + G + ".");
        send(sender, " ");

        send(sender, section("*", profile.getFreeDisplayName()));
        send(sender, "%prefix% " + bullet("Current", BAD + plugin.getDescription().getVersion()));
        send(sender, "%prefix% " + bullet("Latest", OK + freeState.latestVersion));

        if (premiumState.latestVersion != null) {
            send(sender, " ");
            send(sender, section("+", profile.getPremiumDisplayName()));
            send(sender, "%prefix% " + bullet("Latest", Y + premiumState.latestVersion + G + " (Premium)"));
            send(sender, "%prefix% " + bullet("Includes", G + "premium updates + premium support"));
        }

        sendLinks(sender, true);
        send(sender, border());
    }

    private void sendPremiumPromo(CommandSender sender) {
        if (premiumState.latestVersion == null) {
            return;
        }

        send(sender, border());
        send(sender, title(RED + "PREMIUM AVAILABLE"));
        send(sender, "%prefix% " + G + "You are on the " + OK + "latest FREE" + G + " version.");
        send(sender, "%prefix% " + G + "Premium gives " + Y + "earlier updates" + G + " + " + Y + "premium support" + G + ".");
        send(sender, " ");

        send(sender, section("+", profile.getPremiumDisplayName()));
        send(sender, "%prefix% " + bullet("Current", BAD + plugin.getDescription().getVersion()));
        send(sender, "%prefix% " + bullet("Latest", Y + premiumState.latestVersion + G + " (Premium)"));

        send(sender, " ");
        send(sender, section(">", "GET PREMIUM"));
        send(sender, "%prefix% " + BLUE + profile.getPremiumDownloadUrl());
        send(sender, border());
    }

    private void sendPremiumUpdate(CommandSender sender) {
        if (premiumState.latestVersion == null) {
            return;
        }

        send(sender, border());
        send(sender, title(RED + "PREMIUM UPDATE AVAILABLE"));
        send(sender, "%prefix% " + G + "A newer premium build is available on " + BLUE + "SpigotMC" + G + ".");
        send(sender, " ");

        send(sender, section("+", profile.getPremiumDisplayName()));
        send(sender, "%prefix% " + bullet("Current", BAD + plugin.getDescription().getVersion()));
        send(sender, "%prefix% " + bullet("Latest", Y + premiumState.latestVersion + G + " (Premium)"));

        send(sender, " ");
        send(sender, section(">", "DOWNLOAD"));
        send(sender, "%prefix% " + BLUE + profile.getPremiumDownloadUrl());
        send(sender, border());
    }

    private void sendLinks(CommandSender sender, boolean includePremium) {
        send(sender, " ");
        send(sender, section(">", "LINKS"));
        if (profile.getFreeDownloadUrl() != null) {
            send(sender, "%prefix% " + BLUE + "- " + W + "Free: " + BLUE + profile.getFreeDownloadUrl());
        }
        if (includePremium && profile.getPremiumDownloadUrl() != null) {
            send(sender, "%prefix% " + BLUE + "- " + W + "Premium: " + BLUE + profile.getPremiumDownloadUrl());
        }
    }

    private void sendInvalidBuildWarning() {
        CommandSender console = plugin.getServer().getConsoleSender();
        send(console, border());
        send(console, title(RED + "INVALID BUILD MARKER"));
        send(console, "%prefix% " + BAD + "Could not validate this " + profile.getDisplayName() + " build marker.");
        send(console, "%prefix% " + G + "Update checks were skipped so the plugin can continue loading safely.");
        send(console, border());
    }

    private UpdateBuildChannel detectBuildChannel() {
        String markerFieldName = profile.getPremiumMarkerFieldName();
        if (markerFieldName == null) {
            if (profile.hasPremiumChannel() && !profile.hasFreeChannel()) {
                return UpdateBuildChannel.PREMIUM;
            }
            if (profile.hasFreeChannel() && !profile.hasPremiumChannel()) {
                return UpdateBuildChannel.FREE;
            }
            return UpdateBuildChannel.UNKNOWN;
        }

        try {
            Field markerField = findField(plugin.getClass(), markerFieldName);
            if (markerField == null || !String.class.equals(markerField.getType())) {
                return UpdateBuildChannel.INVALID;
            }

            markerField.setAccessible(true);
            Object value = markerField.get(Modifier.isStatic(markerField.getModifiers()) ? null : plugin);
            String marker = value instanceof String ? ((String) value).trim() : "";
            if (marker.isEmpty()) {
                return UpdateBuildChannel.INVALID;
            }
            if (spigotUserPlaceholder().equals(marker)) {
                return UpdateBuildChannel.FREE;
            }
            return marker.matches("[0-9]+") ? UpdateBuildChannel.PREMIUM : UpdateBuildChannel.INVALID;
        } catch (Exception ignored) {
            return UpdateBuildChannel.INVALID;
        }
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && !Object.class.equals(current)) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private boolean isAdminJoinEnabled() {
        String path = profile.getAdminJoinConfigPath();
        if (path == null) {
            return true;
        }

        Boolean fromSettings = readSettingsBoolean(path);
        if (fromSettings != null) {
            return fromSettings;
        }

        try {
            return plugin.getConfig().getBoolean(path, true);
        } catch (Exception ignored) {
            return true;
        }
    }

    private Boolean readSettingsBoolean(String path) {
        try {
            Method getSettings = plugin.getClass().getMethod("getSettings");
            Object settings = getSettings.invoke(plugin);
            return readBoolean(settings, path);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Boolean readBoolean(Object source, String path) {
        if (source == null) {
            return null;
        }

        Boolean value = invokeBoolean(source, "getBoolean", new Class<?>[]{String.class, Boolean.TYPE}, new Object[]{path, true});
        if (value != null) {
            return value;
        }

        value = invokeBoolean(source, "getBoolean", new Class<?>[]{String.class, Boolean.class}, new Object[]{path, Boolean.TRUE});
        if (value != null) {
            return value;
        }

        return invokeBoolean(source, "getBoolean", new Class<?>[]{String.class}, new Object[]{path});
    }

    private Boolean invokeBoolean(Object source, String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            Method method = source.getClass().getMethod(methodName, parameterTypes);
            Object result = method.invoke(source, args);
            return result instanceof Boolean ? (Boolean) result : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isOnCooldown(Player player) {
        long now = System.currentTimeMillis();
        Long last = joinCooldown.get(player.getUniqueId());
        if (last != null && now - last < profile.getJoinCooldownMillis()) {
            return true;
        }
        joinCooldown.put(player.getUniqueId(), now);
        return false;
    }

    private void addTask(SchedulerTask task) {
        tasks.add(task);
    }

    private void send(CommandSender sender, String message) {
        String text = message.replace("%prefix%", profile.getMessagePrefix());
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', text));
    }

    private String border() {
        return D + "&m------------------------------";
    }

    private String title(String right) {
        return BLUE + "* " + W + "%prefix% " + D + "- " + right;
    }

    private String section(String marker, String name) {
        return BLUE + marker + " " + RED + name;
    }

    private String bullet(String key, String value) {
        return BLUE + "> " + G + key + ": " + value;
    }

    private String userAgent() {
        return plugin.getName() + "/" + plugin.getDescription().getVersion() + " BeastLib/" + beastLib.getDescription().getVersion();
    }

    private static String get(String url, String userAgent) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", userAgent);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            return null;
        }

        InputStream inputStream = connection.getInputStream();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            try {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            } finally {
                reader.close();
            }
        } finally {
            inputStream.close();
            connection.disconnect();
        }
    }

    private static boolean isNewer(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        String[] as = a.trim().split("\\.");
        String[] bs = b.trim().split("\\.");

        int len = Math.max(as.length, bs.length);
        for (int i = 0; i < len; i++) {
            int ai = i < as.length ? parseIntSafe(as[i]) : 0;
            int bi = i < bs.length ? parseIntSafe(bs[i]) : 0;
            if (ai > bi) {
                return true;
            }
            if (ai < bi) {
                return false;
            }
        }
        return false;
    }

    private static int parseIntSafe(String s) {
        try {
            String digits = s.replaceAll("[^0-9].*$", "").replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(digits);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String spigotUserPlaceholder() {
        return new String(new char[]{
                '%', '%', '_', '_',
                'U', 'S', 'E', 'R',
                '_', '_', '%', '%'
        });
    }

    private static final class VersionState {
        private volatile boolean updateAvailable;
        private volatile String latestVersion;
    }
}
