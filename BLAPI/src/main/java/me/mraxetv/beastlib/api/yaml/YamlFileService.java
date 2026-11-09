package me.mraxetv.beastlib.api.yaml;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.nodes.Tag;
import dev.dejvokep.boostedyaml.settings.Settings;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class YamlFileService {

    private static final YamlFileService INSTANCE = new YamlFileService();

    private YamlFileService() {
    }

    public static YamlFileService getInstance() {
        return INSTANCE;
    }

    public YamlDocument load(JavaPlugin plugin, String filePath) throws IOException {
        return load(plugin, YamlFileOptions.builder(filePath).build());
    }

    public YamlDocument load(JavaPlugin plugin, String filePath, String resourcePath) throws IOException {
        return load(plugin, YamlFileOptions.builder(filePath)
                .setResourcePath(resourcePath)
                .setRequireDefaultResource(true)
                .build());
    }

    public YamlDocument load(JavaPlugin plugin, String filePath, String resourcePath, boolean autoUpdate) throws IOException {
        return load(plugin, YamlFileOptions.builder(filePath)
                .setResourcePath(resourcePath)
                .setRequireDefaultResource(true)
                .setAutoUpdate(autoUpdate)
                .build());
    }

    public YamlDocument load(JavaPlugin plugin, YamlFileOptions options) throws IOException {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        if (options == null) {
            throw new IllegalArgumentException("options cannot be null");
        }

        File file = resolveFile(plugin, options.getFilePath());
        ensureParentDirectoryExists(file);

        if (!file.exists()) {
            if (options.getResourcePath() != null) {
                copyBundledResource(plugin, options.getResourcePath(), file, options.isRequireDefaultResource());
            } else if (options.isCreateFileIfMissing()) {
                Files.createFile(file.toPath());
            }
        }

        if (!file.exists()) {
            throw new IllegalStateException("YAML file does not exist and could not be created: " + file.getAbsolutePath());
        }

        YamlDocument document;
        try (InputStream resource = openBundledResource(plugin, options.getResourcePath(), options.isRequireDefaultResource())) {
            if (resource != null) {
                document = YamlDocument.create(file, resource, createSettings());
            } else {
                document = YamlDocument.create(file, createSettings());
            }
        }

        if (options.isAutoUpdate() && options.getResourcePath() != null) {
            try (InputStream updateResource = openBundledResource(plugin, options.getResourcePath(), options.isRequireDefaultResource())) {
                if (updateResource != null) {
                    document.update(updateResource, createUpdaterSettings());
                }
            }
        }

        return document;
    }

    public File resolveFile(JavaPlugin plugin, String filePath) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }

        try {
            File dataFolder = plugin.getDataFolder().getCanonicalFile();
            File file = new File(dataFolder, normalizePath(filePath)).getCanonicalFile();

            String rootPath = appendSeparator(dataFolder.getPath());
            String filePathCanonical = appendSeparator(file.getPath());
            if (!filePathCanonical.startsWith(rootPath) && !file.getPath().equals(dataFolder.getPath())) {
                throw new IllegalArgumentException("File path must stay inside the plugin data folder: " + filePath);
            }

            return file;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to resolve YAML file path for " + filePath, ex);
        }
    }

    private void copyBundledResource(JavaPlugin plugin, String resourcePath, File file, boolean required) throws IOException {
        try (InputStream resource = openBundledResource(plugin, resourcePath, required)) {
            if (resource == null) {
                return;
            }
            Files.copy(resource, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private InputStream openBundledResource(JavaPlugin plugin, String resourcePath, boolean required) {
        if (resourcePath == null) {
            return null;
        }

        InputStream resource = plugin.getResource(normalizePath(resourcePath));
        if (resource == null && required) {
            throw new IllegalStateException("Bundled resource not found: " + resourcePath);
        }
        return resource;
    }

    private void ensureParentDirectoryExists(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent == null) {
            return;
        }

        Files.createDirectories(parent.toPath());
    }

    private Settings[] createSettings() {
        DumperSettings.Builder dumperBuilder = DumperSettings.builder()
                .setEncoding(DumperSettings.Encoding.UNICODE);
        applyScalarFormatting(dumperBuilder);

        return new Settings[]{
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(false).build(),
                dumperBuilder.build(),
                createUpdaterSettings()
        };
    }

    private UpdaterSettings createUpdaterSettings() {
        return UpdaterSettings.builder()
                .setKeepAll(true)
                .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS)
                .build();
    }

    private void applyScalarFormatting(DumperSettings.Builder dumperBuilder) {
        if (tryApplyScalarFormatter(dumperBuilder)) {
            return;
        }
        if (tryApplyStringStyle(dumperBuilder)) {
            return;
        }
        dumperBuilder.setScalarStyle(ScalarStyle.PLAIN);
    }

    private boolean tryApplyScalarFormatter(DumperSettings.Builder dumperBuilder) {
        try {
            Class<?> formatterClass = Class.forName("dev.dejvokep.boostedyaml.utils.format.Formatter");
            Method setScalarFormatter = dumperBuilder.getClass().getMethod("setScalarFormatter", formatterClass);

            Object formatter = Proxy.newProxyInstance(
                    formatterClass.getClassLoader(),
                    new Class<?>[]{formatterClass},
                    (proxy, method, args) -> {
                        if (args != null && args.length >= 3 && args[2] != null && "KEY".equals(String.valueOf(args[2]))) {
                            return ScalarStyle.PLAIN;
                        }
                        return args != null && args.length >= 1 && Tag.STR.equals(args[0])
                                ? ScalarStyle.DOUBLE_QUOTED
                                : ScalarStyle.PLAIN;
                    }
            );

            setScalarFormatter.invoke(dumperBuilder, formatter);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private boolean tryApplyStringStyle(DumperSettings.Builder dumperBuilder) {
        try {
            Method setStringStyle = dumperBuilder.getClass().getMethod("setStringStyle", ScalarStyle.class);
            setStringStyle.invoke(dumperBuilder, ScalarStyle.DOUBLE_QUOTED);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private String normalizePath(String path) {
        String normalized = path == null ? null : path.trim().replace('\\', '/');
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be blank.");
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String appendSeparator(String path) {
        return path.endsWith(File.separator) ? path : path + File.separator;
    }
}
