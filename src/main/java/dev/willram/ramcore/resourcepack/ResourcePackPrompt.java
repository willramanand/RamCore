package dev.willram.ramcore.resourcepack;

import dev.willram.ramcore.exception.RamPreconditions;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.HexFormat;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * One resource-pack prompt that can be sent and tracked.
 */
public final class ResourcePackPrompt {
    private final UUID id;
    private final URI uri;
    private final byte[] hash;
    private final Component prompt;
    private final boolean forced;
    private final long timeoutTicks;

    private ResourcePackPrompt(
            @NotNull UUID id,
            @NotNull URI uri,
            @NotNull byte[] hash,
            @Nullable Component prompt,
            boolean forced,
            long timeoutTicks
    ) {
        this.id = requireNonNull(id, "id");
        this.uri = requireNonNull(uri, "uri");
        this.hash = requireNonNull(hash, "hash").clone();
        this.prompt = prompt;
        this.forced = forced;
        this.timeoutTicks = timeoutTicks;
        RamPreconditions.checkArgument(this.hash.length == 20, "resource-pack hash must be 20 bytes", "Use the SHA-1 hash bytes for the pack zip.");
        RamPreconditions.checkArgument(timeoutTicks >= 0L, "resource-pack timeout ticks must be >= 0", "Use 0 to disable timeout tracking.");
    }

    @NotNull
    public static Builder builder(@NotNull URI uri, @NotNull byte[] hash) {
        return new Builder(uri, hash);
    }

    @NotNull
    public static Builder builder(@NotNull URI uri, @NotNull String sha1Hex) {
        return builder(uri, sha1Hex(sha1Hex));
    }

    @NotNull
    public UUID id() {
        return this.id;
    }

    @NotNull
    public URI uri() {
        return this.uri;
    }

    @NotNull
    public byte[] hash() {
        return this.hash.clone();
    }

    @Nullable
    public Component prompt() {
        return this.prompt;
    }

    public boolean forced() {
        return this.forced;
    }

    public long timeoutTicks() {
        return this.timeoutTicks;
    }

    public void send(@NotNull Player player) {
        requireNonNull(player, "player").setResourcePack(this.id, this.uri.toString(), hash(), this.prompt, this.forced);
    }

    @NotNull
    public static byte[] sha1Hex(@NotNull String sha1Hex) {
        requireNonNull(sha1Hex, "sha1Hex");
        String trimmed = sha1Hex.trim();
        RamPreconditions.checkArgument(
                trimmed.length() == 40,
                "resource-pack SHA-1 must be 40 hex characters",
                "Generate the SHA-1 hash for the pack zip and pass it as hex."
        );
        return HexFormat.of().parseHex(trimmed);
    }

    public static final class Builder {
        private UUID id = UUID.randomUUID();
        private final URI uri;
        private final byte[] hash;
        private Component prompt;
        private boolean forced;
        private long timeoutTicks;

        private Builder(URI uri, byte[] hash) {
            this.uri = requireNonNull(uri, "uri");
            this.hash = requireNonNull(hash, "hash").clone();
        }

        @NotNull
        public Builder id(@NotNull UUID id) {
            this.id = requireNonNull(id, "id");
            return this;
        }

        @NotNull
        public Builder prompt(@Nullable Component prompt) {
            this.prompt = prompt;
            return this;
        }

        @NotNull
        public Builder forced(boolean forced) {
            this.forced = forced;
            return this;
        }

        @NotNull
        public Builder timeoutTicks(long timeoutTicks) {
            this.timeoutTicks = timeoutTicks;
            return this;
        }

        @NotNull
        public ResourcePackPrompt build() {
            return new ResourcePackPrompt(this.id, this.uri, this.hash, this.prompt, this.forced, this.timeoutTicks);
        }
    }
}
