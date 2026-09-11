package dev.willram.ramcore.reward;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Resolves the player a {@link RewardContext} is about.
 */
public final class RewardSubjects {

    private RewardSubjects() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Resolves the subject to a player id. Accepts a {@link UUID}, an {@link OfflinePlayer}, or a
     * {@link Player}.
     *
     * @param context the reward context
     * @return the player id, or empty when the subject is none of those
     */
    @NotNull
    public static Optional<UUID> playerId(@NotNull RewardContext context) {
        Object subject = requireNonNull(context, "context").subject();
        if (subject instanceof UUID uuid) {
            return Optional.of(uuid);
        }
        if (subject instanceof OfflinePlayer player) {
            return Optional.of(player.getUniqueId());
        }
        return Optional.empty();
    }

    /**
     * Resolves the subject to an online player.
     *
     * @param context the reward context
     * @return the online player, or empty
     */
    @NotNull
    public static Optional<Player> onlinePlayer(@NotNull RewardContext context) {
        Object subject = requireNonNull(context, "context").subject();
        if (subject instanceof Player player) {
            return Optional.of(player);
        }
        return playerId(context).map(org.bukkit.Bukkit::getPlayer);
    }
}
