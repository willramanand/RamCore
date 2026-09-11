package dev.willram.ramcore.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ResourcePackPrimitivesTest {

    @Test
    public void assetSourceOfBytesIsDefensive() throws IOException {
        byte[] original = {1, 2, 3};
        AssetSource source = AssetSource.ofBytes(original);
        original[0] = 9;
        assertArrayEquals(new byte[]{1, 2, 3}, source.bytes());
    }

    @Test
    public void assetSourceOfString() throws IOException {
        assertArrayEquals("hi".getBytes(StandardCharsets.UTF_8), AssetSource.ofString("hi").bytes());
    }

    @Test
    public void assetSourceOfFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("t.txt");
        Files.writeString(file, "data");
        assertArrayEquals("data".getBytes(StandardCharsets.UTF_8), AssetSource.ofFile(file).bytes());
    }

    @Test
    public void assetSourceOfMissingResourceThrows() {
        AssetSource source = AssetSource.ofResource(getClass().getClassLoader(), "does/not/exist.bin");
        assertThrows(IOException.class, source::bytes);
    }

    @Test
    public void sha1IsStableAndHex() {
        byte[] digest = Sha1.digest("abc".getBytes(StandardCharsets.UTF_8));
        assertEquals(20, digest.length);
        // known SHA-1("abc")
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", Sha1.hex(digest));
    }

    @Test
    public void packFormatsLookup() {
        assertEquals(46, PackFormats.forMinecraft("1.21.4"));
        assertEquals(34, PackFormats.forMinecraft("1.21"));
        // unknown/newer falls to latest; older-between falls to floor
        assertEquals(PackFormats.LATEST, PackFormats.forMinecraft("1.99"));
        assertEquals(46, PackFormats.forMinecraft("1.21.4-pre1".replace("-pre1", "")));
        assertEquals(PackFormats.LATEST, PackFormats.forMinecraft("garbage"));
    }

    @Test
    public void writesPackMeta() {
        String meta = PackWriters.packMeta(46, "My Pack");
        assertTrue(meta.contains("\"pack_format\": 46"));
        assertTrue(meta.contains("\"description\": \"My Pack\""));
    }

    @Test
    public void writesItemModelDefinition() {
        String json = PackWriters.itemModelDefinition(ResourcePackAssetId.of("myplugin", "item/ruby_sword"));
        assertTrue(json.contains("\"type\": \"minecraft:model\""));
        assertTrue(json.contains("\"model\": \"myplugin:item/ruby_sword\""));
    }

    @Test
    public void writesBasicItemModel() {
        String json = PackWriters.basicItemModel(ResourcePackAssetId.of("myplugin", "item/ruby_sword"));
        assertTrue(json.contains("\"parent\": \"minecraft:item/generated\""));
        assertTrue(json.contains("\"layer0\": \"myplugin:item/ruby_sword\""));
    }
}
