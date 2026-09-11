package dev.willram.ramcore.resourcepack;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Objects.requireNonNull;

/**
 * Assembles a resource pack from {@link AssetSource}s, writes {@code pack.mcmeta} and the item-model
 * JSON for content items, zips it deterministically, hashes it (SHA-1), and reports what changed
 * since the last build via a per-file manifest.
 *
 * <p>Deterministic output (sorted entries, fixed timestamps) means identical content produces an
 * identical zip and SHA-1, so clients re-download only on real changes. All work is blocking file
 * I/O — run {@link #buildTo} on an async scheduler in production. Off-server safe and unit-testable.</p>
 */
public final class ResourcePackBuilder {
    // 1980-01-01, the earliest a zip entry timestamp may be; fixed for reproducible archives.
    private static final long FIXED_TIME = 315532800000L;

    private String name = "pack";
    private int packFormat = PackFormats.LATEST;
    private String description = "";
    private final Map<String, AssetSource> files = new TreeMap<>();

    private ResourcePackBuilder() {
    }

    @NotNull
    public static ResourcePackBuilder create() {
        return new ResourcePackBuilder();
    }

    @NotNull
    public ResourcePackBuilder name(@NotNull String name) {
        this.name = requireNonNull(name, "name");
        return this;
    }

    @NotNull
    public ResourcePackBuilder packFormat(int packFormat) {
        RamPreconditions.checkArgument(packFormat >= 0, "packFormat must be >= 0", "pass a valid pack format");
        this.packFormat = packFormat;
        return this;
    }

    @NotNull
    public ResourcePackBuilder minecraftVersion(@NotNull String minecraftVersion) {
        return packFormat(PackFormats.forMinecraft(minecraftVersion));
    }

    @NotNull
    public ResourcePackBuilder description(@NotNull String description) {
        this.description = requireNonNull(description, "description");
        return this;
    }

    /**
     * Adds an arbitrary file at an in-pack path (e.g. {@code assets/ns/sounds.json}).
     *
     * @param pathInPack the path inside the pack (forward slashes, no leading slash or {@code ..})
     * @param source     the bytes
     * @return this builder
     */
    @NotNull
    public ResourcePackBuilder file(@NotNull String pathInPack, @NotNull AssetSource source) {
        String path = normalize(pathInPack);
        this.files.put(path, requireNonNull(source, "source"));
        return this;
    }

    /**
     * Adds a texture at {@code assets/<ns>/textures/<path>.png}.
     *
     * @param id  the texture id ({@code ns:path}); {@code path} may include subdirectories
     * @param png the texture bytes
     * @return this builder
     */
    @NotNull
    public ResourcePackBuilder texture(@NotNull ResourcePackAssetId id, @NotNull AssetSource png) {
        requireNonNull(id, "id");
        return file("assets/" + id.namespace() + "/textures/" + id.path() + ".png", png);
    }

    /**
     * Adds a content item: writes the modern item-model definition, a basic generated model, and the
     * item texture, all keyed off {@code itemId}. The item model referenced by
     * {@code ItemComponentProfile.itemModel(..)} is {@code <ns>:item/<path>}.
     *
     * @param itemId     the item id ({@code ns:path})
     * @param texturePng the item texture bytes
     * @return this builder
     */
    @NotNull
    public ResourcePackBuilder item(@NotNull ResourcePackAssetId itemId, @NotNull AssetSource texturePng) {
        requireNonNull(itemId, "itemId");
        requireNonNull(texturePng, "texturePng");
        String ns = itemId.namespace();
        String path = itemId.path();
        ResourcePackAssetId modelId = ResourcePackAssetId.of(ns, "item/" + path);
        file("assets/" + ns + "/items/" + path + ".json", AssetSource.ofString(PackWriters.itemModelDefinition(modelId)));
        file("assets/" + ns + "/models/item/" + path + ".json", AssetSource.ofString(PackWriters.basicItemModel(modelId)));
        file("assets/" + ns + "/textures/item/" + path + ".png", texturePng);
        return this;
    }

    /**
     * Builds the pack, using a manifest alongside the zip ({@code <zip>.manifest}) for incremental
     * reporting.
     *
     * @param zip the output zip path
     * @return the build report
     * @throws IOException on I/O failure
     */
    @NotNull
    public PackBuildReport buildTo(@NotNull Path zip) throws IOException {
        return buildTo(zip, zip.resolveSibling(zip.getFileName() + ".manifest"));
    }

    /**
     * Builds the pack to {@code zip}, comparing per-file hashes against {@code manifest} for the
     * incremental report and rewriting {@code manifest} afterwards.
     *
     * @param zip      the output zip path
     * @param manifest the manifest path
     * @return the build report
     * @throws IOException on I/O failure
     */
    @NotNull
    public PackBuildReport buildTo(@NotNull Path zip, @NotNull Path manifest) throws IOException {
        requireNonNull(zip, "zip");
        requireNonNull(manifest, "manifest");

        Map<String, byte[]> contents = new TreeMap<>();
        contents.put("pack.mcmeta", PackWriters.packMeta(this.packFormat, this.description).getBytes(StandardCharsets.UTF_8));
        for (Map.Entry<String, AssetSource> entry : this.files.entrySet()) {
            contents.put(entry.getKey(), entry.getValue().bytes());
        }

        Map<String, String> current = new TreeMap<>();
        for (Map.Entry<String, byte[]> entry : contents.entrySet()) {
            current.put(entry.getKey(), Sha1.hex(Sha1.digest(entry.getValue())));
        }

        Map<String, String> previous = PackManifest.read(manifest);
        PackBuildReport report = diff(previous, current, writeAndHash(zip, contents));
        PackManifest.write(manifest, current);
        return report;
    }

    private static String writeAndHash(@NotNull Path zip, @NotNull Map<String, byte[]> contents) throws IOException {
        Path parent = zip.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(zip);
             ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> entry : contents.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setTime(FIXED_TIME);
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return Sha1.hex(Sha1.digest(Files.readAllBytes(zip)));
    }

    private static PackBuildReport diff(@NotNull Map<String, String> previous, @NotNull Map<String, String> current,
                                        @NotNull String sha1Hex) {
        List<String> added = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        List<String> unchanged = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        current.forEach((path, hex) -> {
            String old = previous.get(path);
            if (old == null) {
                added.add(path);
            } else if (old.equals(hex)) {
                unchanged.add(path);
            } else {
                changed.add(path);
            }
        });
        previous.keySet().forEach(path -> {
            if (!current.containsKey(path)) {
                removed.add(path);
            }
        });
        return new PackBuildReport(added, changed, removed, unchanged, sha1Hex);
    }

    /**
     * Builds a {@link ResourcePackMetadata} for a hosted URL from a finished build.
     *
     * @param uri    the download URL
     * @param report the build report (for the SHA-1)
     * @return the metadata
     */
    @NotNull
    public ResourcePackMetadata metadata(@NotNull URI uri, @NotNull PackBuildReport report) {
        return ResourcePackMetadata.builder(this.name, uri)
                .sha1Hex(report.sha1Hex())
                .packFormat(this.packFormat)
                .description(this.description)
                .build();
    }

    private static String normalize(@Nullable String pathInPack) {
        RamPreconditions.checkArgument(pathInPack != null && !pathInPack.isBlank(),
                "in-pack path must not be blank", "pass a non-blank path");
        String path = pathInPack.replace('\\', '/');
        RamPreconditions.checkArgument(!path.startsWith("/"), "in-pack path must not start with '/'",
                "pass a relative path");
        RamPreconditions.checkArgument(!path.contains(".."), "in-pack path must not contain '..'",
                "pass a path without parent traversal");
        return path;
    }
}
