package dev.willram.ramcore.world;

import org.bukkit.block.spawner.SpawnerEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Weighted potential spawn entry.
 */
public record SpawnerWeightedEntry(
        @NotNull SpawnerEntityTemplate template,
        int weight,
        @NotNull SpawnerSpawnRule spawnRule
) {

    public SpawnerWeightedEntry {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(spawnRule, "spawnRule");
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
    }

    @NotNull
    public static SpawnerWeightedEntry of(@NotNull SpawnerEntityTemplate template, int weight) {
        return new SpawnerWeightedEntry(template, weight, SpawnerSpawnRule.ANY_LIGHT);
    }

    @NotNull
    public static SpawnerWeightedEntry fromPaper(@NotNull SpawnerEntry entry) {
        Objects.requireNonNull(entry, "entry");
        return new SpawnerWeightedEntry(
                SpawnerEntityTemplate.snapshot(entry.getSnapshot()),
                entry.getSpawnWeight(),
                SpawnerSpawnRule.fromPaper(entry.getSpawnRule())
        );
    }

    @NotNull
    public SpawnerEntry toPaper() {
        return this.template.snapshot()
                .map(snapshot -> new SpawnerEntry(snapshot, this.weight, this.spawnRule.toPaper()))
                .orElseThrow(() -> new UnsupportedOperationException("Paper spawner entries require an EntitySnapshot; raw SNBT templates need a guarded NMS adapter."));
    }
}
