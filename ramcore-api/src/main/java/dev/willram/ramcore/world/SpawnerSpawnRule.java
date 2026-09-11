package dev.willram.ramcore.world;

import org.bukkit.block.spawner.SpawnRule;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable Paper spawner light rule wrapper.
 */
public record SpawnerSpawnRule(int minBlockLight, int maxBlockLight, int minSkyLight, int maxSkyLight) {

    public static final SpawnerSpawnRule ANY_LIGHT = new SpawnerSpawnRule(0, 15, 0, 15);

    public SpawnerSpawnRule {
        validateLight("minBlockLight", minBlockLight);
        validateLight("maxBlockLight", maxBlockLight);
        validateLight("minSkyLight", minSkyLight);
        validateLight("maxSkyLight", maxSkyLight);
        if (minBlockLight > maxBlockLight) {
            throw new IllegalArgumentException("minBlockLight cannot exceed maxBlockLight");
        }
        if (minSkyLight > maxSkyLight) {
            throw new IllegalArgumentException("minSkyLight cannot exceed maxSkyLight");
        }
    }

    @NotNull
    public static SpawnerSpawnRule fromPaper(@NotNull SpawnRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new SpawnerSpawnRule(rule.getMinBlockLight(), rule.getMaxBlockLight(), rule.getMinSkyLight(), rule.getMaxSkyLight());
    }

    @NotNull
    public SpawnRule toPaper() {
        return new SpawnRule(this.minBlockLight, this.maxBlockLight, this.minSkyLight, this.maxSkyLight);
    }

    private static void validateLight(String name, int value) {
        if (value < 0 || value > 15) {
            throw new IllegalArgumentException(name + " must be between 0 and 15");
        }
    }
}
