package dev.willram.ramcore.cooldown;

import dev.willram.ramcore.data.DataKeyCodec;
import dev.willram.ramcore.exception.RamPreconditions;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Grouped cooldown key for sharing one cooldown store across many actions.
 */
public record CooldownKey(@NotNull String group, @NotNull Object key) {
    public CooldownKey {
        group = RamPreconditions.notBlank(group, "cooldown group");
        requireNonNull(key, "key");
    }

    @NotNull
    public static CooldownKey of(@NotNull String group, @NotNull Object key) {
        return new CooldownKey(group, key);
    }

    @NotNull
    public static CooldownKey player(@NotNull String group, @NotNull Player player) {
        return uuid(group, requireNonNull(player, "player").getUniqueId());
    }

    @NotNull
    public static CooldownKey player(@NotNull String group, @NotNull OfflinePlayer player) {
        return uuid(group, requireNonNull(player, "player").getUniqueId());
    }

    @NotNull
    public static CooldownKey uuid(@NotNull String group, @NotNull UUID uuid) {
        return of(group, uuid);
    }

    /**
     * Key codec for file and SQL backends: {@code <group>:<key>}. On decode the key part becomes a
     * {@link UUID} when it parses as one, otherwise a {@link String}; other key types round-trip as
     * their string form.
     */
    @NotNull
    public static DataKeyCodec<CooldownKey> keyCodec() {
        return new DataKeyCodec<>() {
            @Override
            public @NotNull String encode(@NotNull CooldownKey key) {
                return key.group() + ":" + key.key();
            }

            @Override
            public @NotNull CooldownKey decode(@NotNull String value) {
                int separator = value.indexOf(':');
                RamPreconditions.checkArgument(separator > 0 && separator < value.length() - 1,
                        "malformed cooldown key: " + value,
                        "Keys are written as <group>:<key>; do not hand-edit store files.");
                String group = value.substring(0, separator);
                String rawKey = value.substring(separator + 1);
                try {
                    return of(group, UUID.fromString(rawKey));
                } catch (IllegalArgumentException notUuid) {
                    return of(group, rawKey);
                }
            }
        };
    }

    @Override
    public String toString() {
        return this.group + ":" + this.key;
    }
}
