package dev.willram.ramcore.world;

import org.bukkit.block.spawner.SpawnerEntry;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.spawner.BaseSpawner;
import org.bukkit.spawner.Spawner;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Validated spawner configuration that can be applied to block or minecart spawners.
 */
public final class SpawnerConfig {
    private final SpawnerEntityTemplate spawnedTemplate;
    private final Integer delay;
    private final Integer minSpawnDelay;
    private final Integer maxSpawnDelay;
    private final Integer spawnCount;
    private final Integer maxNearbyEntities;
    private final Integer requiredPlayerRange;
    private final Integer spawnRange;
    private final List<SpawnerWeightedEntry> potentialSpawns;

    private SpawnerConfig(Builder builder) {
        this.spawnedTemplate = builder.spawnedTemplate;
        this.delay = builder.delay;
        this.minSpawnDelay = builder.minSpawnDelay;
        this.maxSpawnDelay = builder.maxSpawnDelay;
        this.spawnCount = builder.spawnCount;
        this.maxNearbyEntities = builder.maxNearbyEntities;
        this.requiredPlayerRange = builder.requiredPlayerRange;
        this.spawnRange = builder.spawnRange;
        this.potentialSpawns = List.copyOf(builder.potentialSpawns);
        validateDelays(this.minSpawnDelay, this.maxSpawnDelay);
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public static SpawnerConfig capture(@NotNull BaseSpawner spawner) {
        Objects.requireNonNull(spawner, "spawner");
        Builder builder = builder()
                .delay(spawner.getDelay())
                .requiredPlayerRange(spawner.getRequiredPlayerRange())
                .spawnRange(spawner.getSpawnRange());

        EntitySnapshot spawnedEntity = spawner.getSpawnedEntity();
        if (spawnedEntity != null) {
            builder.spawnedTemplate(SpawnerEntityTemplate.snapshot(spawnedEntity));
        } else if (spawner.getSpawnedType() != null) {
            builder.spawnedTemplate(SpawnerEntityTemplate.of(spawner.getSpawnedType()));
        }

        for (SpawnerEntry entry : spawner.getPotentialSpawns()) {
            builder.potentialSpawn(SpawnerWeightedEntry.fromPaper(entry));
        }

        if (spawner instanceof Spawner tickingSpawner) {
            builder.minSpawnDelay(tickingSpawner.getMinSpawnDelay())
                    .maxSpawnDelay(tickingSpawner.getMaxSpawnDelay())
                    .spawnCount(tickingSpawner.getSpawnCount())
                    .maxNearbyEntities(tickingSpawner.getMaxNearbyEntities());
        }
        return builder.build();
    }

    @NotNull
    public Optional<SpawnerEntityTemplate> spawnedTemplate() {
        return Optional.ofNullable(this.spawnedTemplate);
    }

    @NotNull
    public List<SpawnerWeightedEntry> potentialSpawns() {
        return this.potentialSpawns;
    }

    public void apply(@NotNull BaseSpawner spawner) {
        Objects.requireNonNull(spawner, "spawner");
        if (this.spawnedTemplate != null) {
            this.spawnedTemplate.snapshot().ifPresentOrElse(
                    spawner::setSpawnedEntity,
                    () -> spawner.setSpawnedType(this.spawnedTemplate.type())
            );
        }
        if (this.delay != null) {
            spawner.setDelay(this.delay);
        }
        if (this.requiredPlayerRange != null) {
            spawner.setRequiredPlayerRange(this.requiredPlayerRange);
        }
        if (this.spawnRange != null) {
            spawner.setSpawnRange(this.spawnRange);
        }
        if (!this.potentialSpawns.isEmpty()) {
            List<SpawnerEntry> entries = this.potentialSpawns.stream().map(SpawnerWeightedEntry::toPaper).toList();
            spawner.setPotentialSpawns(entries);
        }

        if (spawner instanceof Spawner tickingSpawner) {
            if (this.minSpawnDelay != null) {
                tickingSpawner.setMinSpawnDelay(this.minSpawnDelay);
            }
            if (this.maxSpawnDelay != null) {
                tickingSpawner.setMaxSpawnDelay(this.maxSpawnDelay);
            }
            if (this.spawnCount != null) {
                tickingSpawner.setSpawnCount(this.spawnCount);
            }
            if (this.maxNearbyEntities != null) {
                tickingSpawner.setMaxNearbyEntities(this.maxNearbyEntities);
            }
        }
    }

    @NotNull
    public Map<String, String> describe() {
        Map<String, String> properties = new LinkedHashMap<>();
        if (this.spawnedTemplate != null) {
            properties.put("spawnedType", this.spawnedTemplate.type().key().asString());
            this.spawnedTemplate.rawSnbt().ifPresent(snbt -> properties.put("spawnedRawSnbt", snbt));
        }
        put(properties, "delay", this.delay);
        put(properties, "minSpawnDelay", this.minSpawnDelay);
        put(properties, "maxSpawnDelay", this.maxSpawnDelay);
        put(properties, "spawnCount", this.spawnCount);
        put(properties, "maxNearbyEntities", this.maxNearbyEntities);
        put(properties, "requiredPlayerRange", this.requiredPlayerRange);
        put(properties, "spawnRange", this.spawnRange);
        properties.put("potentialSpawns", Integer.toString(this.potentialSpawns.size()));
        return properties;
    }

    private static void put(Map<String, String> properties, String key, Integer value) {
        if (value != null) {
            properties.put(key, Integer.toString(value));
        }
    }

    private static void validateDelays(Integer minSpawnDelay, Integer maxSpawnDelay) {
        if (minSpawnDelay != null && maxSpawnDelay != null && minSpawnDelay > maxSpawnDelay) {
            throw new IllegalArgumentException("minSpawnDelay cannot exceed maxSpawnDelay");
        }
    }

    public static final class Builder {
        private SpawnerEntityTemplate spawnedTemplate;
        private Integer delay;
        private Integer minSpawnDelay;
        private Integer maxSpawnDelay;
        private Integer spawnCount;
        private Integer maxNearbyEntities;
        private Integer requiredPlayerRange;
        private Integer spawnRange;
        private final List<SpawnerWeightedEntry> potentialSpawns = new ArrayList<>();

        private Builder() {
        }

        @NotNull
        public Builder spawnedTemplate(@NotNull SpawnerEntityTemplate spawnedTemplate) {
            this.spawnedTemplate = Objects.requireNonNull(spawnedTemplate, "spawnedTemplate");
            return this;
        }

        @NotNull
        public Builder delay(int delay) {
            this.delay = nonNegative("delay", delay);
            return this;
        }

        @NotNull
        public Builder minSpawnDelay(int minSpawnDelay) {
            this.minSpawnDelay = nonNegative("minSpawnDelay", minSpawnDelay);
            validateDelays(this.minSpawnDelay, this.maxSpawnDelay);
            return this;
        }

        @NotNull
        public Builder maxSpawnDelay(int maxSpawnDelay) {
            this.maxSpawnDelay = nonNegative("maxSpawnDelay", maxSpawnDelay);
            validateDelays(this.minSpawnDelay, this.maxSpawnDelay);
            return this;
        }

        @NotNull
        public Builder spawnCount(int spawnCount) {
            this.spawnCount = positive("spawnCount", spawnCount);
            return this;
        }

        @NotNull
        public Builder maxNearbyEntities(int maxNearbyEntities) {
            this.maxNearbyEntities = positive("maxNearbyEntities", maxNearbyEntities);
            return this;
        }

        @NotNull
        public Builder requiredPlayerRange(int requiredPlayerRange) {
            this.requiredPlayerRange = nonNegative("requiredPlayerRange", requiredPlayerRange);
            return this;
        }

        @NotNull
        public Builder spawnRange(int spawnRange) {
            this.spawnRange = nonNegative("spawnRange", spawnRange);
            return this;
        }

        @NotNull
        public Builder potentialSpawn(@NotNull SpawnerWeightedEntry entry) {
            this.potentialSpawns.add(Objects.requireNonNull(entry, "entry"));
            return this;
        }

        @NotNull
        public SpawnerConfig build() {
            return new SpawnerConfig(this);
        }

        private static int nonNegative(String name, int value) {
            if (value < 0) {
                throw new IllegalArgumentException(name + " cannot be negative");
            }
            return value;
        }

        private static int positive(String name, int value) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }
    }
}
