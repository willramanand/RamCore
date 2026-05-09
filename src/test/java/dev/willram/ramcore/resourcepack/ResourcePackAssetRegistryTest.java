package dev.willram.ramcore.resourcepack;

import dev.willram.ramcore.exception.ApiMisuseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class ResourcePackAssetRegistryTest {

    @Test
    public void parsesAssetIdsWithPaths() {
        ResourcePackAssetId id = ResourcePackAssetId.parse("example:item/fire_sword");

        assertEquals("example", id.namespace());
        assertEquals("item/fire_sword", id.path());
        assertEquals("example:item/fire_sword", id.toString());
    }

    @Test
    public void registersAndFindsModelAssets() {
        ResourcePackAssetRegistry registry = ResourcePackAssetRegistry.create();
        ResourcePackAsset asset = ResourcePackAsset.builder(ResourcePackAssetId.parse("example:item/fire_sword"), ResourcePackAssetType.ITEM_MODEL)
                .customModelData(101)
                .itemModelKey(ResourcePackAssetId.parse("example:item/fire_sword"))
                .metadata("source", "items.json")
                .build();

        registry.register("Example", asset);

        assertSame(asset, registry.require(ResourcePackAssetId.parse("example:item/fire_sword")));
        assertSame(asset, registry.byCustomModelData(101).orElseThrow());
        assertSame(asset, registry.byItemModel(ResourcePackAssetId.parse("example:item/fire_sword")).orElseThrow());
        assertEquals(1, registry.byType(ResourcePackAssetType.ITEM_MODEL).size());
    }

    @Test
    public void duplicateRuntimeKeysFailFast() {
        ResourcePackAssetRegistry registry = ResourcePackAssetRegistry.create();
        registry.register("A", ResourcePackAsset.customModelData(ResourcePackAssetId.parse("example:item/first"), 101));

        try {
            registry.register("B", ResourcePackAsset.customModelData(ResourcePackAssetId.parse("example:item/second"), 101));
        } catch (ApiMisuseException e) {
            assertTrue(e.problem().contains("custom model data already registered"));
            return;
        }

        throw new AssertionError("expected ApiMisuseException");
    }

    @Test
    public void multipleGlyphsCanShareOneFontKey() {
        ResourcePackAssetRegistry registry = ResourcePackAssetRegistry.create();
        ResourcePackAssetId font = ResourcePackAssetId.parse("example:font/icons");
        ResourcePackAsset first = ResourcePackAsset.fontGlyph(ResourcePackAssetId.parse("example:glyph/check"), font, "\uE001");
        ResourcePackAsset second = ResourcePackAsset.fontGlyph(ResourcePackAssetId.parse("example:glyph/x"), font, "\uE002");

        registry.register("Example", first);
        registry.register("Example", second);

        assertSame(first, registry.byFontGlyph(font, "\uE001").orElseThrow());
        assertSame(second, registry.byFontGlyph(font, "\uE002").orElseThrow());
    }

    @Test
    public void failedRegistrationDoesNotLeavePartialIndexes() {
        ResourcePackAssetRegistry registry = ResourcePackAssetRegistry.create();
        ResourcePackAssetId model = ResourcePackAssetId.parse("example:item/shared");
        registry.register("A", ResourcePackAsset.builder(ResourcePackAssetId.parse("example:item/first"), ResourcePackAssetType.ITEM_MODEL)
                .itemModelKey(model)
                .build());

        try {
            registry.register("B", ResourcePackAsset.builder(ResourcePackAssetId.parse("example:item/second"), ResourcePackAssetType.ITEM_MODEL)
                    .customModelData(202)
                    .itemModelKey(model)
                    .build());
        } catch (ApiMisuseException ignored) {
            assertTrue(registry.byCustomModelData(202).isEmpty());
            return;
        }

        throw new AssertionError("expected ApiMisuseException");
    }
}
