package dev.willram.ramcore.cooldown;

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

    @Override
    public String toString() {
        return this.group + ":" + this.key;
    }
}
