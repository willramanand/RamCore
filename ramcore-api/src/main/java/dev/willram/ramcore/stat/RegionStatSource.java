package dev.willram.ramcore.stat;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.region.RegionTracker;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNull;

/**
 * A {@link StatSource} that contributes modifiers for the regions a player currently stands in,
 * using the enter/exit membership tracked by {@link RegionTracker} (task 1.5). Because a
 * {@code RuleRegion} carries no metadata, region stat modifiers are registered here, keyed by
 * region id.
 */
public final class RegionStatSource implements StatSource {

    private final RegionTracker tracker;
    private final Map<ContentId, List<StatModifier>> byRegion = new ConcurrentHashMap<>();

    public RegionStatSource(@NotNull RegionTracker tracker) {
        this.tracker = requireNonNull(tracker, "tracker");
    }

    /**
     * Adds a modifier granted while inside a region.
     *
     * @param regionId the region id
     * @param modifier the modifier
     */
    public void put(@NotNull ContentId regionId, @NotNull StatModifier modifier) {
        requireNonNull(regionId, "regionId");
        requireNonNull(modifier, "modifier");
        this.byRegion.computeIfAbsent(regionId, k -> new CopyOnWriteArrayList<>()).add(modifier);
    }

    /**
     * Removes all modifiers for a region.
     *
     * @param regionId the region id
     */
    public void clear(@NotNull ContentId regionId) {
        this.byRegion.remove(requireNonNull(regionId, "regionId"));
    }

    @Override
    @NotNull
    public List<StatModifier> modifiers(@NotNull Player player) {
        List<StatModifier> out = new ArrayList<>();
        for (ContentId regionId : this.tracker.current(player.getUniqueId())) {
            List<StatModifier> modifiers = this.byRegion.get(regionId);
            if (modifiers != null) {
                out.addAll(modifiers);
            }
        }
        return out;
    }
}
