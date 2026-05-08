package dev.willram.ramcore.party;

import dev.willram.ramcore.scheduler.Schedulers;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Folia-aware group teleport helper.
 */
public final class PartyTeleport {

    public static int teleport(@NotNull PartyGroup party, @NotNull Function<UUID, @Nullable Player> players, @NotNull Location location) {
        requireNonNull(party, "party");
        requireNonNull(players, "players");
        requireNonNull(location, "location");
        int scheduled = 0;
        for (UUID member : party.members()) {
            Player player = players.apply(member);
            if (player == null) {
                continue;
            }
            Schedulers.run(player, () -> player.teleport(location));
            scheduled++;
        }
        return scheduled;
    }

    private PartyTeleport() {
    }
}
