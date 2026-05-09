package dev.willram.ramcore.resourcepack;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.HexFormat;

import static java.util.Objects.requireNonNull;

/**
 * Server-side metadata for a distributed resource pack.
 */
public record ResourcePackMetadata(
        @NotNull String name,
        @NotNull URI uri,
        @Nullable byte[] sha1,
        int packFormat,
        @Nullable String version,
        @Nullable String description
) {
    public ResourcePackMetadata {
        requireNonNull(name, "name");
        requireNonNull(uri, "uri");
        RamPreconditions.checkArgument(!name.isBlank(), "resource-pack name must not be blank", "Use a human-readable pack name.");
        RamPreconditions.checkArgument(packFormat >= 0, "resource-pack format must be >= 0", "Use the pack_format from pack.mcmeta.");
        sha1 = sha1 == null ? null : sha1.clone();
    }

    @NotNull
    public static Builder builder(@NotNull String name, @NotNull URI uri) {
        return new Builder(name, uri);
    }

    @Nullable
    @Override
    public byte[] sha1() {
        return this.sha1 == null ? null : this.sha1.clone();
    }

    @Nullable
    public String sha1Hex() {
        return this.sha1 == null ? null : HexFormat.of().formatHex(this.sha1);
    }

    public static final class Builder {
        private final String name;
        private final URI uri;
        private byte[] sha1;
        private int packFormat;
        private String version;
        private String description;

        private Builder(String name, URI uri) {
            this.name = requireNonNull(name, "name");
            this.uri = requireNonNull(uri, "uri");
        }

        @NotNull
        public Builder sha1(@Nullable byte[] sha1) {
            this.sha1 = sha1 == null ? null : sha1.clone();
            return this;
        }

        @NotNull
        public Builder sha1Hex(@NotNull String sha1Hex) {
            this.sha1 = ResourcePackPrompt.sha1Hex(sha1Hex);
            return this;
        }

        @NotNull
        public Builder packFormat(int packFormat) {
            this.packFormat = packFormat;
            return this;
        }

        @NotNull
        public Builder version(@Nullable String version) {
            this.version = version;
            return this;
        }

        @NotNull
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        @NotNull
        public ResourcePackMetadata build() {
            return new ResourcePackMetadata(this.name, this.uri, this.sha1, this.packFormat, this.version, this.description);
        }
    }
}
