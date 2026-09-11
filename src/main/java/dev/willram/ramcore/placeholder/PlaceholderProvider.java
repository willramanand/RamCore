package dev.willram.ramcore.placeholder;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves placeholder strings for a player. One provider owns one id namespace; the text after the
 * id is passed as {@code params}.
 *
 * <p>Stability: stable. Providers must tolerate an offline player and return {@code null} for
 * anything they do not recognise.</p>
 */
public interface PlaceholderProvider {

    /**
     * The provider id, matched against the first segment of a placeholder key.
     *
     * @return the id (lowercase, no underscores)
     */
    @NotNull
    String id();

    /**
     * Resolves the placeholder.
     *
     * @param player the player (possibly offline)
     * @param params the text after the id, for example {@code party_size}
     * @return the value, or null when unrecognised
     */
    @Nullable
    String resolve(@NotNull OfflinePlayer player, @NotNull String params);
}
