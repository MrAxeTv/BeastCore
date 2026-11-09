package me.mraxetv.beastlib.api.update;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class UpdateProductProfile {

    private final String productKey;
    private final String displayName;
    private final String messagePrefix;
    private final String freeProjectSlugOrId;
    private final String freeDownloadUrl;
    private final String freeDisplayName;
    private final Integer premiumResourceId;
    private final String premiumDownloadUrl;
    private final String premiumDisplayName;
    private final Set<String> premiumFeatureKeys;
    private final String adminPermission;
    private final String adminJoinConfigPath;
    private final String premiumMarkerFieldName;
    private final long startupDelayTicks;
    private final long freeStartupMessageDelayTicks;
    private final long recheckTicks;
    private final long joinCooldownMillis;

    private UpdateProductProfile(Builder builder) {
        this.productKey = requireText(builder.productKey, "productKey");
        this.displayName = textOr(builder.displayName, this.productKey);
        this.messagePrefix = textOr(builder.messagePrefix, "&7[" + this.displayName + "&7]");
        this.freeProjectSlugOrId = trimToNull(builder.freeProjectSlugOrId);
        this.freeDownloadUrl = trimToNull(builder.freeDownloadUrl);
        this.freeDisplayName = textOr(builder.freeDisplayName, "FREE");
        this.premiumResourceId = builder.premiumResourceId;
        this.premiumDownloadUrl = trimToNull(builder.premiumDownloadUrl);
        this.premiumDisplayName = textOr(builder.premiumDisplayName, this.displayName + " Plus");
        this.premiumFeatureKeys = Collections.unmodifiableSet(new LinkedHashSet<>(builder.premiumFeatureKeys));
        this.adminPermission = trimToNull(builder.adminPermission);
        this.adminJoinConfigPath = trimToNull(builder.adminJoinConfigPath);
        this.premiumMarkerFieldName = trimToNull(builder.premiumMarkerFieldName);
        this.startupDelayTicks = builder.startupDelayTicks;
        this.freeStartupMessageDelayTicks = builder.freeStartupMessageDelayTicks;
        this.recheckTicks = builder.recheckTicks;
        this.joinCooldownMillis = builder.joinCooldownMillis;
    }

    public static Builder builder(String productKey) {
        return new Builder(productKey);
    }

    public String getProductKey() {
        return productKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }

    public String getFreeProjectSlugOrId() {
        return freeProjectSlugOrId;
    }

    public String getFreeDownloadUrl() {
        return freeDownloadUrl;
    }

    public String getFreeDisplayName() {
        return freeDisplayName;
    }

    public Integer getPremiumResourceId() {
        return premiumResourceId;
    }

    public String getPremiumDownloadUrl() {
        return premiumDownloadUrl;
    }

    public String getPremiumDisplayName() {
        return premiumDisplayName;
    }

    public Set<String> getPremiumFeatureKeys() {
        return premiumFeatureKeys;
    }

    public boolean isPremiumFeature(String featureKey) {
        String normalized = normalizeFeatureKey(featureKey);
        return normalized != null && premiumFeatureKeys.contains(normalized);
    }

    public String getAdminPermission() {
        return adminPermission;
    }

    public String getAdminJoinConfigPath() {
        return adminJoinConfigPath;
    }

    public String getPremiumMarkerFieldName() {
        return premiumMarkerFieldName;
    }

    public long getStartupDelayTicks() {
        return startupDelayTicks;
    }

    public long getFreeStartupMessageDelayTicks() {
        return freeStartupMessageDelayTicks;
    }

    public long getRecheckTicks() {
        return recheckTicks;
    }

    public long getJoinCooldownMillis() {
        return joinCooldownMillis;
    }

    public boolean hasFreeChannel() {
        return freeProjectSlugOrId != null;
    }

    public boolean hasPremiumChannel() {
        return premiumResourceId != null && premiumResourceId > 0;
    }

    private static String requireText(String value, String field) {
        String text = trimToNull(value);
        if (text == null) {
            throw new IllegalArgumentException(field + " cannot be empty");
        }
        return text;
    }

    private static String textOr(String value, String fallback) {
        String text = trimToNull(value);
        return text == null ? fallback : text;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeFeatureKey(String featureKey) {
        String normalized = trimToNull(featureKey);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(Locale.ENGLISH);
    }

    public static final class Builder {
        private final String productKey;
        private String displayName;
        private String messagePrefix;
        private String freeProjectSlugOrId;
        private String freeDownloadUrl;
        private String freeDisplayName;
        private Integer premiumResourceId;
        private String premiumDownloadUrl;
        private String premiumDisplayName;
        private final Set<String> premiumFeatureKeys = new LinkedHashSet<>();
        private String adminPermission;
        private String adminJoinConfigPath;
        private String premiumMarkerFieldName;
        private long startupDelayTicks = 20L * 3L;
        private long freeStartupMessageDelayTicks = 1L;
        private long recheckTicks = 20L * 60L * 10L;
        private long joinCooldownMillis = 10_000L;

        private Builder(String productKey) {
            this.productKey = productKey;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder messagePrefix(String messagePrefix) {
            this.messagePrefix = messagePrefix;
            return this;
        }

        public Builder freeProject(String freeProjectSlugOrId) {
            this.freeProjectSlugOrId = freeProjectSlugOrId;
            return this;
        }

        public Builder freeDownloadUrl(String freeDownloadUrl) {
            this.freeDownloadUrl = freeDownloadUrl;
            return this;
        }

        public Builder freeDisplayName(String freeDisplayName) {
            this.freeDisplayName = freeDisplayName;
            return this;
        }

        public Builder premiumResource(int premiumResourceId) {
            this.premiumResourceId = premiumResourceId;
            return this;
        }

        public Builder premiumDownloadUrl(String premiumDownloadUrl) {
            this.premiumDownloadUrl = premiumDownloadUrl;
            return this;
        }

        public Builder premiumDisplayName(String premiumDisplayName) {
            this.premiumDisplayName = premiumDisplayName;
            return this;
        }

        public Builder premiumFeature(String featureKey) {
            String normalized = normalizeFeatureKey(featureKey);
            if (normalized != null) {
                this.premiumFeatureKeys.add(normalized);
            }
            return this;
        }

        public Builder premiumFeatures(String... featureKeys) {
            if (featureKeys == null) {
                return this;
            }
            for (String featureKey : featureKeys) {
                premiumFeature(featureKey);
            }
            return this;
        }

        public Builder premiumFeatures(Collection<String> featureKeys) {
            if (featureKeys == null) {
                return this;
            }
            for (String featureKey : featureKeys) {
                premiumFeature(featureKey);
            }
            return this;
        }

        public Builder adminPermission(String adminPermission) {
            this.adminPermission = adminPermission;
            return this;
        }

        public Builder adminJoinConfigPath(String adminJoinConfigPath) {
            this.adminJoinConfigPath = adminJoinConfigPath;
            return this;
        }

        public Builder premiumMarkerFieldName(String premiumMarkerFieldName) {
            this.premiumMarkerFieldName = premiumMarkerFieldName;
            return this;
        }

        public Builder startupDelayTicks(long startupDelayTicks) {
            this.startupDelayTicks = startupDelayTicks;
            return this;
        }

        public Builder freeStartupMessageDelayTicks(long freeStartupMessageDelayTicks) {
            this.freeStartupMessageDelayTicks = freeStartupMessageDelayTicks;
            return this;
        }

        public Builder recheckTicks(long recheckTicks) {
            this.recheckTicks = recheckTicks;
            return this;
        }

        public Builder joinCooldownMillis(long joinCooldownMillis) {
            this.joinCooldownMillis = joinCooldownMillis;
            return this;
        }

        public UpdateProductProfile build() {
            return new UpdateProductProfile(this);
        }
    }
}
