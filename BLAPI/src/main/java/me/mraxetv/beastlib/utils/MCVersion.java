package me.mraxetv.beastlib.utils;

import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifies the Minecraft version used by the current Bukkit server.
 *
 * <p>This class is owned by BeastLib and deliberately does not rely on the
 * NBT API's version mappings. Plugins depending on BeastLib can therefore use
 * these mappings even when the NBT API has not added support for a new
 * Minecraft release yet.</p>
 */
public enum MCVersion {

    /**
     * An unrecognized version. Its ID is higher than every known version so
     * feature checks can use the newest compatible code path where possible.
     */
    UNKNOWN(Integer.MAX_VALUE),

    MC1_7_R4(174),
    MC1_8_R3(183),
    MC1_9_R1(191),
    MC1_9_R2(192),
    MC1_10_R1(1101),
    MC1_11_R1(1111),
    MC1_12_R1(1121),
    MC1_13_R1(1131),
    MC1_13_R2(1132),
    MC1_14_R1(1141),
    MC1_15_R1(1151),
    MC1_16_R1(1161),
    MC1_16_R2(1162),
    MC1_16_R3(1163),
    MC1_17_R1(1171),
    MC1_18_R1(1181, true),
    MC1_18_R2(1182, true),
    MC1_19_R1(1191, true),
    MC1_19_R2(1192, true),
    MC1_19_R3(1193, true),
    MC1_20_R1(1201, true),
    MC1_20_R2(1202, true),
    MC1_20_R3(1203, true),
    MC1_20_R4(1204, true),
    MC1_21_R1(1211, true),
    MC1_21_R2(1212, true),
    MC1_21_R3(1213, true),
    MC1_21_R4(1214, true),
    MC1_21_R5(1215, true),
    MC1_21_R6(1216, true),
    MC1_21_R7(1217, true),
    MC26_1(260100, true),
    MC26_2(260200, true),
    MC26_3(260300, true);

    private static final Pattern CRAFTBUKKIT_REVISION_PATTERN =
            Pattern.compile("(?:^|\\.)(v\\d+_\\d+_R\\d+)(?:\\.|$)");
    private static final Pattern MINECRAFT_VERSION_PATTERN =
            Pattern.compile("(?<![\\d.])(\\d+(?:\\.\\d+){1,3})(?![\\d.])");

    private static final Map<String, MCVersion> VERSION_TO_REVISION = createVersionMappings();

    private static volatile MCVersion version;
    private static volatile Logger logger = Logger.getLogger("BeastLib");
    private static Boolean hasGsonSupport;
    private static Boolean isForgePresent;
    private static Boolean isNeoForgePresent;
    private static Boolean isFabricPresent;
    private static Boolean isFoliaPresent;

    private final int versionId;
    private final boolean mojangMapping;

    MCVersion(int versionId) {
        this(versionId, false);
    }

    MCVersion(int versionId, boolean mojangMapping) {
        this.versionId = versionId;
        this.mojangMapping = mojangMapping;
    }

    /**
     * Returns the numeric ID used for version comparisons.
     *
     * @return the comparable version ID
     */
    public int getVersionId() {
        return versionId;
    }

    /**
     * Returns whether this version uses Mojang-mapped method names.
     *
     * @return {@code true} when Mojang mappings apply
     */
    public boolean isMojangMapping() {
        return mojangMapping;
    }

    /**
     * Returns the legacy CraftBukkit revision package represented by this
     * constant.
     *
     * @return a package component such as {@code v1_20_R3}
     */
    public String getPackageName() {
        if (this == UNKNOWN) {
            Server server = Bukkit.getServer();
            if (server != null) {
                Matcher matcher = CRAFTBUKKIT_REVISION_PATTERN.matcher(
                        server.getClass().getPackage().getName()
                );
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return name().replaceFirst("^MC", "v");
    }

    /**
     * Checks whether the current server is at least the supplied version.
     *
     * @param minimumVersion the minimum supported version
     * @return {@code true} when the current version is equal to or newer
     */
    public static boolean isAtLeastVersion(MCVersion minimumVersion) {
        Objects.requireNonNull(minimumVersion, "minimumVersion");
        return getVersion().getVersionId() >= minimumVersion.getVersionId();
    }

    /**
     * Checks whether the current server is newer than the supplied version.
     *
     * @param comparedVersion the version to compare against
     * @return {@code true} only when the current version is newer
     */
    public static boolean isNewerThan(MCVersion comparedVersion) {
        Objects.requireNonNull(comparedVersion, "comparedVersion");
        return getVersion().getVersionId() > comparedVersion.getVersionId();
    }

    /**
     * Returns the version of the running Bukkit server.
     *
     * @return the detected version, or {@link #UNKNOWN} when it is not mapped
     */
    public static MCVersion getVersion() {
        MCVersion detectedVersion = version;
        if (detectedVersion != null) {
            return detectedVersion;
        }

        synchronized (MCVersion.class) {
            if (version != null) {
                return version;
            }

            Server server = Bukkit.getServer();
            if (server == null) {
                return UNKNOWN;
            }

            String serverPackage = server.getClass().getPackage().getName();
            version = fromServerPackage(serverPackage);

            if (version == UNKNOWN) {
                version = fromVersionString(server.getBukkitVersion());
            }
            if (version == UNKNOWN) {
                version = fromVersionString(server.getVersion());
            }

            if (version == UNKNOWN) {
                logger.warning("[BeastLib] This server version (" + server.getBukkitVersion()
                        + ") is not mapped. BeastLib will treat it as newer than all known versions.");
            } else {
                logger.info("[BeastLib] Minecraft version " + version.name() + " detected.");
            }
            return version;
        }
    }

    /**
     * Resolves a Minecraft release string without requiring a running server.
     * This accepts Bukkit strings such as {@code 1.21.4-R0.1-SNAPSHOT} and
     * longer server descriptions containing a Minecraft version.
     *
     * @param versionString the version text to resolve
     * @return the mapped version, or {@link #UNKNOWN}
     */
    public static MCVersion fromVersionString(String versionString) {
        if (versionString == null || versionString.trim().isEmpty()) {
            return UNKNOWN;
        }

        Matcher matcher = MINECRAFT_VERSION_PATTERN.matcher(versionString);
        while (matcher.find()) {
            String release = matcher.group(1);
            MCVersion exactVersion = VERSION_TO_REVISION.get(release);
            if (exactVersion != null) {
                return exactVersion;
            }

            MCVersion releaseLineVersion = findLatestReleaseLineVersion(release);
            if (releaseLineVersion != null) {
                return releaseLineVersion;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns whether Gson is available to the server class loader.
     *
     * @return {@code true} when Gson is present
     */
    public static synchronized boolean hasGsonSupport() {
        if (hasGsonSupport == null) {
            hasGsonSupport = isClassPresent("com.google.gson.Gson");
        }
        return hasGsonSupport;
    }

    /**
     * Returns whether Fabric is present.
     *
     * @return {@code true} when Fabric is present
     */
    public static synchronized boolean isFabricPresent() {
        if (isFabricPresent == null) {
            isFabricPresent = isClassPresent("net.fabricmc.api.ModInitializer");
        }
        return isFabricPresent;
    }

    /**
     * Returns whether Forge is present.
     *
     * @return {@code true} when Forge is present
     */
    public static synchronized boolean isForgePresent() {
        if (isForgePresent == null) {
            isForgePresent = isClassPresent("net.minecraftforge.fml.common.Loader")
                    || isClassPresent("cpw.mods.fml.common.Loader");
        }
        return isForgePresent;
    }

    /**
     * Returns whether NeoForge is present.
     *
     * @return {@code true} when NeoForge is present
     */
    public static synchronized boolean isNeoForgePresent() {
        if (isNeoForgePresent == null) {
            isNeoForgePresent = isClassPresent("net.neoforged.neoforge.common.NeoForge");
        }
        return isNeoForgePresent;
    }

    /**
     * Returns whether Folia is present.
     *
     * @return {@code true} when Folia is present
     */
    public static synchronized boolean isFoliaPresent() {
        if (isFoliaPresent == null) {
            isFoliaPresent = isClassPresent("io.papermc.paper.threadedregions.RegionizedServer");
        }
        return isFoliaPresent;
    }

    /**
     * Returns the logger used by this utility.
     *
     * @return the current logger
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Replaces the logger used by this utility.
     *
     * @param replacementLogger the non-null replacement
     */
    public static void replaceLogger(Logger replacementLogger) {
        logger = Objects.requireNonNull(replacementLogger, "replacementLogger");
    }

    private static MCVersion fromServerPackage(String serverPackage) {
        if (serverPackage == null) {
            return UNKNOWN;
        }

        Matcher matcher = CRAFTBUKKIT_REVISION_PATTERN.matcher(serverPackage);
        if (!matcher.find()) {
            return UNKNOWN;
        }

        String enumName = matcher.group(1).replaceFirst("^v", "MC");
        try {
            return valueOf(enumName);
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }

    private static MCVersion findLatestReleaseLineVersion(String release) {
        int firstDot = release.indexOf('.');
        int secondDot = firstDot < 0 ? -1 : release.indexOf('.', firstDot + 1);
        String releaseLine = secondDot < 0 ? release : release.substring(0, secondDot);

        // Only extrapolate a future patch when the release line itself is a
        // known mapping. This avoids treating unsupported legacy revisions,
        // such as 1.8 R1, as the newest revision from that release line.
        if (!VERSION_TO_REVISION.containsKey(releaseLine)) {
            return null;
        }

        MCVersion latestVersion = null;
        for (Map.Entry<String, MCVersion> entry : VERSION_TO_REVISION.entrySet()) {
            String mappedRelease = entry.getKey();
            if (!mappedRelease.equals(releaseLine) && !mappedRelease.startsWith(releaseLine + ".")) {
                continue;
            }
            if (latestVersion == null || entry.getValue().versionId > latestVersion.versionId) {
                latestVersion = entry.getValue();
            }
        }
        return latestVersion;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, MCVersion.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError | SecurityException ignored) {
            return false;
        }
    }

    private static Map<String, MCVersion> createVersionMappings() {
        Map<String, MCVersion> mappings = new HashMap<>();

        mappings.put("1.7.10", MC1_7_R4);
        mappings.put("1.8.4", MC1_8_R3);
        mappings.put("1.8.5", MC1_8_R3);
        mappings.put("1.8.6", MC1_8_R3);
        mappings.put("1.8.7", MC1_8_R3);
        mappings.put("1.8.8", MC1_8_R3);
        mappings.put("1.9", MC1_9_R1);
        mappings.put("1.9.1", MC1_9_R1);
        mappings.put("1.9.2", MC1_9_R1);
        mappings.put("1.9.3", MC1_9_R2);
        mappings.put("1.9.4", MC1_9_R2);
        mappings.put("1.10", MC1_10_R1);
        mappings.put("1.10.1", MC1_10_R1);
        mappings.put("1.10.2", MC1_10_R1);
        mappings.put("1.11", MC1_11_R1);
        mappings.put("1.11.1", MC1_11_R1);
        mappings.put("1.11.2", MC1_11_R1);
        mappings.put("1.12", MC1_12_R1);
        mappings.put("1.12.1", MC1_12_R1);
        mappings.put("1.12.2", MC1_12_R1);
        mappings.put("1.13", MC1_13_R1);
        mappings.put("1.13.1", MC1_13_R2);
        mappings.put("1.13.2", MC1_13_R2);
        mappings.put("1.14", MC1_14_R1);
        mappings.put("1.14.1", MC1_14_R1);
        mappings.put("1.14.2", MC1_14_R1);
        mappings.put("1.14.3", MC1_14_R1);
        mappings.put("1.14.4", MC1_14_R1);
        mappings.put("1.15", MC1_15_R1);
        mappings.put("1.15.1", MC1_15_R1);
        mappings.put("1.15.2", MC1_15_R1);
        mappings.put("1.16", MC1_16_R1);
        mappings.put("1.16.1", MC1_16_R1);
        mappings.put("1.16.2", MC1_16_R2);
        mappings.put("1.16.3", MC1_16_R2);
        mappings.put("1.16.4", MC1_16_R3);
        mappings.put("1.16.5", MC1_16_R3);
        mappings.put("1.17", MC1_17_R1);
        mappings.put("1.17.1", MC1_17_R1);
        mappings.put("1.18", MC1_18_R1);
        mappings.put("1.18.1", MC1_18_R1);
        mappings.put("1.18.2", MC1_18_R2);
        mappings.put("1.19", MC1_19_R1);
        mappings.put("1.19.1", MC1_19_R1);
        mappings.put("1.19.2", MC1_19_R1);
        mappings.put("1.19.3", MC1_19_R2);
        mappings.put("1.19.4", MC1_19_R3);
        mappings.put("1.20", MC1_20_R1);
        mappings.put("1.20.1", MC1_20_R1);
        mappings.put("1.20.2", MC1_20_R2);
        mappings.put("1.20.3", MC1_20_R3);
        mappings.put("1.20.4", MC1_20_R3);
        mappings.put("1.20.5", MC1_20_R4);
        mappings.put("1.20.6", MC1_20_R4);
        mappings.put("1.21", MC1_21_R1);
        mappings.put("1.21.1", MC1_21_R1);
        mappings.put("1.21.2", MC1_21_R2);
        mappings.put("1.21.3", MC1_21_R2);
        mappings.put("1.21.4", MC1_21_R3);
        mappings.put("1.21.5", MC1_21_R4);
        mappings.put("1.21.6", MC1_21_R5);
        mappings.put("1.21.7", MC1_21_R5);
        mappings.put("1.21.8", MC1_21_R5);
        mappings.put("1.21.9", MC1_21_R6);
        mappings.put("1.21.10", MC1_21_R6);
        mappings.put("1.21.11", MC1_21_R7);
        mappings.put("26.1", MC26_1);
        mappings.put("26.2", MC26_2);
        mappings.put("26.3", MC26_3);

        return Collections.unmodifiableMap(mappings);
    }
}
