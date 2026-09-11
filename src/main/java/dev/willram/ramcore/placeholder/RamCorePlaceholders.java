package dev.willram.ramcore.placeholder;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.cooldown.CooldownTracker;
import dev.willram.ramcore.objective.ObjectiveSubject;
import dev.willram.ramcore.objective.ObjectiveTracker;
import dev.willram.ramcore.party.PartyManager;
import dev.willram.ramcore.region.RegionRuleEngine;
import dev.willram.ramcore.serialize.Position;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * Builds the opt-in built-in {@code ramcore} {@link PlaceholderProvider}. RamCore holds no gameplay
 * instances, so a consumer wires the ones it wants:
 *
 * <pre>{@code
 * PlaceholderProvider provider = RamCorePlaceholders.builder()
 *         .parties(partyManager)
 *         .cooldowns("combat", combatTracker)
 *         .objectives(objectiveTracker)
 *         .regions(regionEngine)
 *         .build();
 * registry.register(provider);
 * }</pre>
 *
 * <p>Params: {@code party_size}, {@code party_leader}, {@code cooldown_<name>_<key>} (remaining
 * seconds), {@code objective_<namespace:value>_<task>} (current amount), {@code region} (highest
 * priority region id at the player's location). Unknown or unwired params resolve to null.</p>
 */
public final class RamCorePlaceholders {

    private RamCorePlaceholders() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private PartyManager parties;
        private final Map<String, CooldownTracker<String>> cooldowns = new LinkedHashMap<>();
        private ObjectiveTracker objectives;
        private RegionRuleEngine regions;

        @NotNull
        public Builder parties(@NotNull PartyManager parties) {
            this.parties = requireNonNull(parties, "parties");
            return this;
        }

        @NotNull
        public Builder cooldowns(@NotNull String name, @NotNull CooldownTracker<String> tracker) {
            this.cooldowns.put(requireNonNull(name, "name"), requireNonNull(tracker, "tracker"));
            return this;
        }

        @NotNull
        public Builder objectives(@NotNull ObjectiveTracker objectives) {
            this.objectives = requireNonNull(objectives, "objectives");
            return this;
        }

        @NotNull
        public Builder regions(@NotNull RegionRuleEngine regions) {
            this.regions = requireNonNull(regions, "regions");
            return this;
        }

        @NotNull
        public PlaceholderProvider build() {
            return new RamCoreProvider(this.parties, Map.copyOf(this.cooldowns), this.objectives, this.regions);
        }
    }

    private record RamCoreProvider(@Nullable PartyManager parties, @NotNull Map<String, CooldownTracker<String>> cooldowns,
                                   @Nullable ObjectiveTracker objectives, @Nullable RegionRuleEngine regions) implements PlaceholderProvider {

        @NotNull
        @Override
        public String id() {
            return "ramcore";
        }

        @Nullable
        @Override
        public String resolve(@NotNull OfflinePlayer player, @NotNull String params) {
            if (params.equals("party_size")) {
                return this.parties == null ? null : this.parties.partyOf(player.getUniqueId()).map(party -> String.valueOf(party.size())).orElse("0");
            }
            if (params.equals("party_leader")) {
                if (this.parties == null) {
                    return null;
                }
                return this.parties.partyOf(player.getUniqueId())
                        .map(party -> {
                            String name = Bukkit.getOfflinePlayer(party.leader()).getName();
                            return name != null ? name : party.leader().toString();
                        })
                        .orElse("");
            }
            if (params.startsWith("cooldown_")) {
                return resolveCooldown(params.substring("cooldown_".length()));
            }
            if (params.startsWith("objective_")) {
                return resolveObjective(player, params.substring("objective_".length()));
            }
            if (params.equals("region")) {
                return resolveRegion(player);
            }
            return null;
        }

        @Nullable
        private String resolveCooldown(@NotNull String rest) {
            int underscore = rest.indexOf('_');
            if (underscore < 0) {
                return null;
            }
            String name = rest.substring(0, underscore);
            String key = rest.substring(underscore + 1);
            CooldownTracker<String> tracker = this.cooldowns.get(name);
            if (tracker == null) {
                return null;
            }
            return String.valueOf(Math.max(0, tracker.remainingTime(key, TimeUnit.SECONDS)));
        }

        @Nullable
        private String resolveObjective(@NotNull OfflinePlayer player, @NotNull String rest) {
            if (this.objectives == null) {
                return null;
            }
            int underscore = rest.lastIndexOf('_');
            if (underscore < 0) {
                return null;
            }
            String objectiveId = rest.substring(0, underscore);
            String task = rest.substring(underscore + 1);
            ContentId id;
            try {
                id = ContentId.parse(objectiveId);
            } catch (RuntimeException invalid) {
                return null;
            }
            ObjectiveSubject subject = ObjectiveSubject.player(player.getUniqueId());
            return this.objectives.existingProgress(subject, id)
                    .map(progress -> String.valueOf(progress.current(task)))
                    .orElse("0");
        }

        @Nullable
        private String resolveRegion(@NotNull OfflinePlayer player) {
            if (this.regions == null) {
                return null;
            }
            Player online = player.getPlayer();
            if (online == null) {
                return "";
            }
            Position position = Position.of(online.getLocation());
            return this.regions.regionsAt(position).stream()
                    .findFirst()
                    .map(region -> region.id().toString())
                    .orElse("");
        }
    }
}
