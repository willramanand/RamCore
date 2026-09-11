package dev.willram.ramcore.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ResourcePackBuilderTest {

    private static Map<String, byte[]> readZip(Path zip) throws IOException {
        Map<String, byte[]> out = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(Files.readAllBytes(zip)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                out.put(entry.getName(), zis.readAllBytes());
            }
        }
        return out;
    }

    @Test
    public void buildsItemPackWithAllFiles(@TempDir Path dir) throws IOException {
        Path zip = dir.resolve("pack.zip");
        PackBuildReport report = ResourcePackBuilder.create()
                .name("Test Pack")
                .packFormat(46)
                .description("hi")
                .item(ResourcePackAssetId.of("myplugin", "ruby_sword"), AssetSource.ofBytes(new byte[]{1, 2, 3}))
                .buildTo(zip);

        Map<String, byte[]> entries = readZip(zip);
        assertTrue(entries.containsKey("pack.mcmeta"));
        assertTrue(entries.containsKey("assets/myplugin/items/ruby_sword.json"));
        assertTrue(entries.containsKey("assets/myplugin/models/item/ruby_sword.json"));
        assertTrue(entries.containsKey("assets/myplugin/textures/item/ruby_sword.png"));
        assertArrayEquals(new byte[]{1, 2, 3}, entries.get("assets/myplugin/textures/item/ruby_sword.png"));

        String def = new String(entries.get("assets/myplugin/items/ruby_sword.json"), StandardCharsets.UTF_8);
        assertTrue(def.contains("myplugin:item/ruby_sword"));

        // first build: everything added
        assertEquals(4, report.added().size());
        assertTrue(report.removed().isEmpty());
        assertEquals(40, report.sha1Hex().length());
        assertTrue(report.dirty());
    }

    @Test
    public void deterministicSha1ForSameContent(@TempDir Path dir) throws IOException {
        Path a = dir.resolve("a.zip");
        Path b = dir.resolve("b.zip");
        String sha1A = ResourcePackBuilder.create().packFormat(46)
                .file("assets/ns/x.txt", AssetSource.ofString("hello")).buildTo(a).sha1Hex();
        String sha1B = ResourcePackBuilder.create().packFormat(46)
                .file("assets/ns/x.txt", AssetSource.ofString("hello")).buildTo(b).sha1Hex();
        assertEquals(sha1A, sha1B);
    }

    @Test
    public void incrementalReportTracksChanges(@TempDir Path dir) throws IOException {
        Path zip = dir.resolve("pack.zip");
        Path manifest = dir.resolve("pack.manifest");

        ResourcePackBuilder.create().packFormat(46)
                .file("a.txt", AssetSource.ofString("one"))
                .file("b.txt", AssetSource.ofString("two"))
                .buildTo(zip, manifest);

        // second build: change a, remove b, add c; pack.mcmeta unchanged
        PackBuildReport report = ResourcePackBuilder.create().packFormat(46)
                .file("a.txt", AssetSource.ofString("ONE"))
                .file("c.txt", AssetSource.ofString("three"))
                .buildTo(zip, manifest);

        assertTrue(report.changed().contains("a.txt"), () -> report.changed().toString());
        assertTrue(report.added().contains("c.txt"));
        assertTrue(report.removed().contains("b.txt"));
        assertTrue(report.unchanged().contains("pack.mcmeta"));
    }

    @Test
    public void metadataFromReport(@TempDir Path dir) throws IOException {
        Path zip = dir.resolve("pack.zip");
        ResourcePackBuilder builder = ResourcePackBuilder.create().name("Meta").packFormat(46).description("d");
        PackBuildReport report = builder.file("a.txt", AssetSource.ofString("x")).buildTo(zip);

        ResourcePackMetadata metadata = builder.metadata(URI.create("https://example.com/pack.zip"), report);
        assertEquals("Meta", metadata.name());
        assertEquals(46, metadata.packFormat());
        assertEquals(report.sha1Hex(), metadata.sha1Hex());
    }

    @Test
    public void rejectsUnsafePaths() {
        ResourcePackBuilder builder = ResourcePackBuilder.create();
        assertThrows(RuntimeException.class, () -> builder.file("/abs.txt", AssetSource.ofString("x")));
        assertThrows(RuntimeException.class, () -> builder.file("../escape.txt", AssetSource.ofString("x")));
    }
}
