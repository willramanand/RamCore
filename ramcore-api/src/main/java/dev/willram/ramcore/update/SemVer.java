package dev.willram.ramcore.update;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * A tolerant semantic version: {@code major.minor.patch} with an optional pre-release suffix. A
 * leading {@code v} and build metadata ({@code +...}) are ignored. Pre-release versions sort below
 * their release ({@code 1.0.0-rc1 < 1.0.0}); pre-release identifiers compare lexically.
 *
 * @param major   major version
 * @param minor   minor version
 * @param patch   patch version
 * @param preRelease pre-release identifier, or empty for a release
 */
public record SemVer(int major, int minor, int patch, @NotNull String preRelease) implements Comparable<SemVer> {

    public SemVer {
        requireNonNull(preRelease, "preRelease");
    }

    /**
     * Parses a version string, tolerating a leading {@code v} and build metadata.
     *
     * @param raw the version string
     * @return the parsed version
     * @throws IllegalArgumentException when the string is not a version
     */
    @NotNull
    public static SemVer parse(@NotNull String raw) {
        requireNonNull(raw, "raw");
        String value = raw.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }
        int plus = value.indexOf('+');
        if (plus >= 0) {
            value = value.substring(0, plus);
        }
        String preRelease = "";
        int dash = value.indexOf('-');
        if (dash >= 0) {
            preRelease = value.substring(dash + 1);
            value = value.substring(0, dash);
        }
        String[] parts = value.split("\\.");
        if (parts.length == 0 || parts.length > 3) {
            throw new IllegalArgumentException("not a semantic version: " + raw);
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new SemVer(major, minor, patch, preRelease);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a semantic version: " + raw, e);
        }
    }

    public boolean isPreRelease() {
        return !this.preRelease.isEmpty();
    }

    @Override
    public int compareTo(@NotNull SemVer other) {
        int result = Integer.compare(this.major, other.major);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(this.minor, other.minor);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(this.patch, other.patch);
        if (result != 0) {
            return result;
        }
        if (this.preRelease.isEmpty() && other.preRelease.isEmpty()) {
            return 0;
        }
        if (this.preRelease.isEmpty()) {
            return 1; // release > pre-release
        }
        if (other.preRelease.isEmpty()) {
            return -1;
        }
        return this.preRelease.compareTo(other.preRelease);
    }

    @Override
    public String toString() {
        return this.major + "." + this.minor + "." + this.patch + (this.preRelease.isEmpty() ? "" : "-" + this.preRelease);
    }
}
