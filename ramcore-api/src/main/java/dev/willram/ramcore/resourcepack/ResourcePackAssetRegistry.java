package dev.willram.ramcore.resourcepack;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Owner-tracked registry for resource-pack asset ids and runtime keys.
 */
public final class ResourcePackAssetRegistry {
    private final Map<ResourcePackAssetId, ResourcePackAssetEntry> entries = new LinkedHashMap<>();
    private final Map<Integer, ResourcePackAssetId> customModelData = new LinkedHashMap<>();
    private final Map<ResourcePackAssetId, ResourcePackAssetId> itemModels = new LinkedHashMap<>();
    private final Map<ResourcePackAssetId, ResourcePackAssetId> sounds = new LinkedHashMap<>();
    private final Map<String, ResourcePackAssetId> fontGlyphs = new LinkedHashMap<>();

    @NotNull
    public static ResourcePackAssetRegistry create() {
        return new ResourcePackAssetRegistry();
    }

    @NotNull
    public ResourcePackAssetEntry register(@NotNull String owner, @NotNull ResourcePackAsset asset) {
        requireNonNull(owner, "owner");
        requireNonNull(asset, "asset");
        RamPreconditions.checkArgument(!owner.isBlank(), "resource-pack asset owner must not be blank", "Use plugin name or module id.");
        RamPreconditions.checkArgument(
                !this.entries.containsKey(asset.id()),
                "resource-pack asset id already registered: " + asset.id(),
                "Choose a different asset id or unregister the previous owner first."
        );

        ensureUnique(asset.customModelData(), this.customModelData, "custom model data");
        ensureUnique(asset.itemModelKey(), this.itemModels, "item model key");
        ensureUnique(asset.soundKey(), this.sounds, "sound key");
        ensureUnique(fontGlyphIndex(asset), this.fontGlyphs, "font glyph");

        ResourcePackAssetEntry entry = new ResourcePackAssetEntry(owner, asset);
        this.entries.put(asset.id(), entry);
        putIndex(asset.customModelData(), this.customModelData, asset.id());
        putIndex(asset.itemModelKey(), this.itemModels, asset.id());
        putIndex(asset.soundKey(), this.sounds, asset.id());
        putIndex(fontGlyphIndex(asset), this.fontGlyphs, asset.id());
        return entry;
    }

    @NotNull
    public Optional<ResourcePackAsset> get(@NotNull ResourcePackAssetId id) {
        ResourcePackAssetEntry entry = this.entries.get(requireNonNull(id, "id"));
        return entry == null ? Optional.empty() : Optional.of(entry.asset());
    }

    @NotNull
    public ResourcePackAsset require(@NotNull ResourcePackAssetId id) {
        return get(id).orElseThrow(() -> RamPreconditions.misuse(
                "resource-pack asset id not registered: " + id,
                "Register the asset before lookup or check contains(id) first."
        ));
    }

    public boolean contains(@NotNull ResourcePackAssetId id) {
        return this.entries.containsKey(requireNonNull(id, "id"));
    }

    @NotNull
    public Optional<ResourcePackAsset> byCustomModelData(int customModelData) {
        ResourcePackAssetId id = this.customModelData.get(customModelData);
        return id == null ? Optional.empty() : get(id);
    }

    @NotNull
    public Optional<ResourcePackAsset> byItemModel(@NotNull ResourcePackAssetId itemModelKey) {
        ResourcePackAssetId id = this.itemModels.get(requireNonNull(itemModelKey, "itemModelKey"));
        return id == null ? Optional.empty() : get(id);
    }

    @NotNull
    public Optional<ResourcePackAsset> bySound(@NotNull ResourcePackAssetId soundKey) {
        ResourcePackAssetId id = this.sounds.get(requireNonNull(soundKey, "soundKey"));
        return id == null ? Optional.empty() : get(id);
    }

    @NotNull
    public Optional<ResourcePackAsset> byFontKey(@NotNull ResourcePackAssetId fontKey) {
        requireNonNull(fontKey, "fontKey");
        return this.entries.values().stream()
                .map(ResourcePackAssetEntry::asset)
                .filter(asset -> fontKey.equals(asset.fontKey()))
                .findFirst();
    }

    @NotNull
    public Optional<ResourcePackAsset> byFontGlyph(@NotNull ResourcePackAssetId fontKey, @NotNull String glyph) {
        ResourcePackAssetId id = this.fontGlyphs.get(fontGlyphIndex(fontKey, glyph));
        return id == null ? Optional.empty() : get(id);
    }

    @NotNull
    public List<ResourcePackAsset> byType(@NotNull ResourcePackAssetType type) {
        requireNonNull(type, "type");
        return this.entries.values().stream()
                .map(ResourcePackAssetEntry::asset)
                .filter(asset -> asset.type() == type)
                .toList();
    }

    @NotNull
    public Set<ResourcePackAssetId> ids() {
        return Set.copyOf(this.entries.keySet());
    }

    @NotNull
    public Collection<ResourcePackAssetEntry> entries() {
        return List.copyOf(this.entries.values());
    }

    public int unregisterOwner(@NotNull String owner) {
        requireNonNull(owner, "owner");
        List<ResourcePackAssetId> removed = this.entries.entrySet().stream()
                .filter(entry -> entry.getValue().owner().equals(owner))
                .map(Map.Entry::getKey)
                .toList();
        removed.forEach(this::unregister);
        return removed.size();
    }

    public boolean unregister(@NotNull ResourcePackAssetId id) {
        ResourcePackAssetEntry removed = this.entries.remove(requireNonNull(id, "id"));
        if (removed == null) {
            return false;
        }

        ResourcePackAsset asset = removed.asset();
        if (asset.customModelData() != null) {
            this.customModelData.remove(asset.customModelData());
        }
        if (asset.itemModelKey() != null) {
            this.itemModels.remove(asset.itemModelKey());
        }
        if (asset.soundKey() != null) {
            this.sounds.remove(asset.soundKey());
        }
        String fontGlyphIndex = fontGlyphIndex(asset);
        if (fontGlyphIndex != null) {
            this.fontGlyphs.remove(fontGlyphIndex);
        }
        return true;
    }

    public void clear() {
        this.entries.clear();
        this.customModelData.clear();
        this.itemModels.clear();
        this.sounds.clear();
        this.fontGlyphs.clear();
    }

    private static <K> void ensureUnique(K key, Map<K, ResourcePackAssetId> index, String subject) {
        if (key == null) {
            return;
        }

        ResourcePackAssetId existing = index.get(key);
        RamPreconditions.checkArgument(
                existing == null,
                "resource-pack " + subject + " already registered: " + key,
                "Keep runtime resource-pack keys unique; existing asset is " + existing + "."
        );
    }

    private static <K> void putIndex(K key, Map<K, ResourcePackAssetId> index, ResourcePackAssetId assetId) {
        if (key == null) {
            return;
        }
        index.put(key, assetId);
    }

    private static String fontGlyphIndex(ResourcePackAsset asset) {
        if (asset.fontKey() == null || asset.glyph() == null) {
            return null;
        }
        return fontGlyphIndex(asset.fontKey(), asset.glyph());
    }

    private static String fontGlyphIndex(ResourcePackAssetId fontKey, String glyph) {
        return requireNonNull(fontKey, "fontKey") + "\u0000" + requireNonNull(glyph, "glyph");
    }
}
