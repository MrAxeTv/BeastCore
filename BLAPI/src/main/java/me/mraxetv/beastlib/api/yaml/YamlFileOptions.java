package me.mraxetv.beastlib.api.yaml;

import java.util.Objects;

public final class YamlFileOptions {

    private final String filePath;
    private final String resourcePath;
    private final boolean autoUpdate;
    private final boolean createFileIfMissing;
    private final boolean requireDefaultResource;

    private YamlFileOptions(Builder builder) {
        this.filePath = normalizePath(builder.filePath);
        this.resourcePath = builder.resourcePath == null ? null : normalizePath(builder.resourcePath);
        this.autoUpdate = builder.autoUpdate;
        this.createFileIfMissing = builder.createFileIfMissing;
        this.requireDefaultResource = builder.requireDefaultResource;
    }

    public static Builder builder(String filePath) {
        return new Builder(filePath);
    }

    public String getFilePath() {
        return filePath;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public boolean isCreateFileIfMissing() {
        return createFileIfMissing;
    }

    public boolean isRequireDefaultResource() {
        return requireDefaultResource;
    }

    private static String normalizePath(String path) {
        Objects.requireNonNull(path, "path");

        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be blank.");
        }

        return normalized;
    }

    public static final class Builder {

        private final String filePath;
        private String resourcePath;
        private boolean autoUpdate;
        private boolean createFileIfMissing = true;
        private boolean requireDefaultResource;

        private Builder(String filePath) {
            this.filePath = filePath;
        }

        public Builder setResourcePath(String resourcePath) {
            this.resourcePath = resourcePath;
            return this;
        }

        public Builder setAutoUpdate(boolean autoUpdate) {
            this.autoUpdate = autoUpdate;
            return this;
        }

        public Builder setCreateFileIfMissing(boolean createFileIfMissing) {
            this.createFileIfMissing = createFileIfMissing;
            return this;
        }

        public Builder setRequireDefaultResource(boolean requireDefaultResource) {
            this.requireDefaultResource = requireDefaultResource;
            return this;
        }

        public YamlFileOptions build() {
            return new YamlFileOptions(this);
        }
    }
}
