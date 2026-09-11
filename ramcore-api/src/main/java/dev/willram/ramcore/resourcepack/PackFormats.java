package dev.willram.ramcore.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Maps Minecraft versions to resource-pack {@code pack_format} numbers. A small, best-effort table;
 * callers that know their exact server version should override the format explicitly on
 * {@link ResourcePackBuilder}.
 */
public final class PackFormats {

    // version "major.minor.patch" (patch optional) -> pack_format. Extend as new versions land.
    private static final NavigableMap<int[], Integer> TABLE = new TreeMap<>(PackFormats::compareVersion);

    static {
        put("1.20.5", 32);
        put("1.20.6", 32);
        put("1.21", 34);
        put("1.21.1", 34);
        put("1.21.2", 42);
        put("1.21.3", 42);
        put("1.21.4", 46);
        put("1.21.5", 55);
        put("1.21.6", 63);
        put("1.21.7", 63);
        put("1.21.8", 63);
    }

    /** The newest {@code pack_format} in the table; the default when no version is given. */
    public static final int LATEST = 63;

    private PackFormats() {
    }

    /**
     * The {@code pack_format} for a Minecraft version, or {@link #LATEST} if the version is unknown
     * or newer than the table.
     *
     * @param minecraftVersion a version like {@code "1.21.4"}
     * @return the pack format
     */
    public static int forMinecraft(@NotNull String minecraftVersion) {
        int[] parsed = parse(minecraftVersion);
        if (parsed == null) {
            return LATEST;
        }
        Integer exact = TABLE.get(parsed);
        if (exact != null) {
            return exact;
        }
        var floor = TABLE.floorEntry(parsed);
        return floor != null ? floor.getValue() : LATEST;
    }

    private static void put(String version, int format) {
        TABLE.put(parse(version), format);
    }

    private static int[] parse(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }
        String[] parts = version.trim().split("\\.");
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new int[]{major, minor, patch};
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    private static int compareVersion(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            int cmp = Integer.compare(a[i], b[i]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }
}
