package dev.willram.ramcore.resourcepack;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * One tracked resource-pack asset and the runtime keys it exposes.
 */
public final class ResourcePackAsset {
    private final ResourcePackAssetId id;
    private final ResourcePackAssetType type;
    private final Integer customModelData;
    private final ResourcePackAssetId itemModelKey;
    private final ResourcePackAssetId soundKey;
    private final ResourcePackAssetId fontKey;
    private final String glyph;
    private final Map<String, String> metadata;

    private ResourcePackAsset(
            @NotNull ResourcePackAssetId id,
            @NotNull ResourcePackAssetType type,
            @Nullable Integer customModelData,
            @Nullable ResourcePackAssetId itemModelKey,
            @Nullable ResourcePackAssetId soundKey,
            @Nullable ResourcePackAssetId fontKey,
            @Nullable String glyph,
            @NotNull Map<String, String> metadata
    ) {
        this.id = requireNonNull(id, "id");
        this.type = requireNonNull(type, "type");
        this.customModelData = customModelData;
        this.itemModelKey = itemModelKey;
        this.soundKey = soundKey;
        this.fontKey = fontKey;
        this.glyph = glyph;
        this.metadata = Map.copyOf(metadata);
    }

    @NotNull
    public static ResourcePackAsset customModelData(@NotNull ResourcePackAssetId id, int customModelData) {
        RamPreconditions.checkArgument(
                customModelData > 0,
                "custom model data must be positive",
                "Use the positive integer assigned in your resource pack."
        );
        return builder(id, ResourcePackAssetType.CUSTOM_MODEL_DATA)
                .customModelData(customModelData)
                .build();
    }

    @NotNull
    public static ResourcePackAsset itemModel(@NotNull ResourcePackAssetId id) {
        return builder(id, ResourcePackAssetType.ITEM_MODEL)
                .itemModelKey(id)
                .build();
    }

    @NotNull
    public static ResourcePackAsset sound(@NotNull ResourcePackAssetId id) {
        return builder(id, ResourcePackAssetType.SOUND)
                .soundKey(id)
                .build();
    }

    @NotNull
    public static ResourcePackAsset fontGlyph(@NotNull ResourcePackAssetId id, @NotNull ResourcePackAssetId fontKey, @NotNull String glyph) {
        return builder(id, ResourcePackAssetType.FONT_GLYPH)
                .fontKey(fontKey)
                .glyph(glyph)
                .build();
    }

    @NotNull
    public static Builder builder(@NotNull ResourcePackAssetId id, @NotNull ResourcePackAssetType type) {
        return new Builder(id, type);
    }

    @NotNull
    public ResourcePackAssetId id() {
        return this.id;
    }

    @NotNull
    public ResourcePackAssetType type() {
        return this.type;
    }

    @Nullable
    public Integer customModelData() {
        return this.customModelData;
    }

    @Nullable
    public ResourcePackAssetId itemModelKey() {
        return this.itemModelKey;
    }

    @Nullable
    public ResourcePackAssetId soundKey() {
        return this.soundKey;
    }

    @Nullable
    public ResourcePackAssetId fontKey() {
        return this.fontKey;
    }

    @Nullable
    public String glyph() {
        return this.glyph;
    }

    @NotNull
    public Map<String, String> metadata() {
        return this.metadata;
    }

    public static final class Builder {
        private final ResourcePackAssetId id;
        private final ResourcePackAssetType type;
        private Integer customModelData;
        private ResourcePackAssetId itemModelKey;
        private ResourcePackAssetId soundKey;
        private ResourcePackAssetId fontKey;
        private String glyph;
        private final Map<String, String> metadata = new LinkedHashMap<>();

        private Builder(ResourcePackAssetId id, ResourcePackAssetType type) {
            this.id = requireNonNull(id, "id");
            this.type = requireNonNull(type, "type");
        }

        @NotNull
        public Builder customModelData(int customModelData) {
            RamPreconditions.checkArgument(
                    customModelData > 0,
                    "custom model data must be positive",
                    "Use the positive integer assigned in your resource pack."
            );
            this.customModelData = customModelData;
            return this;
        }

        @NotNull
        public Builder itemModelKey(@NotNull ResourcePackAssetId itemModelKey) {
            this.itemModelKey = requireNonNull(itemModelKey, "itemModelKey");
            return this;
        }

        @NotNull
        public Builder soundKey(@NotNull ResourcePackAssetId soundKey) {
            this.soundKey = requireNonNull(soundKey, "soundKey");
            return this;
        }

        @NotNull
        public Builder fontKey(@NotNull ResourcePackAssetId fontKey) {
            this.fontKey = requireNonNull(fontKey, "fontKey");
            return this;
        }

        @NotNull
        public Builder glyph(@NotNull String glyph) {
            requireNonNull(glyph, "glyph");
            RamPreconditions.checkArgument(
                    !glyph.isEmpty(),
                    "font glyph must not be empty",
                    "Use the exact glyph string or codepoint exposed by the resource pack."
            );
            this.glyph = glyph;
            return this;
        }

        @NotNull
        public Builder metadata(@NotNull String key, @NotNull String value) {
            this.metadata.put(RamPreconditions.notBlank(key, "metadata key"), requireNonNull(value, "value"));
            return this;
        }

        @NotNull
        public Builder metadata(@NotNull Map<String, String> metadata) {
            requireNonNull(metadata, "metadata").forEach(this::metadata);
            return this;
        }

        @NotNull
        public ResourcePackAsset build() {
            return new ResourcePackAsset(
                    this.id,
                    this.type,
                    this.customModelData,
                    this.itemModelKey,
                    this.soundKey,
                    this.fontKey,
                    this.glyph,
                    this.metadata
            );
        }
    }
}
